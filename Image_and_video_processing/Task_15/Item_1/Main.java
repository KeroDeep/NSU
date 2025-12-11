import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel videoPanel;
    private static final Object videoLock = new Object();
    private static FileManager currentFileManager = null;
    
    private static double threshold = 0.3;
    private static int frameCount = 5;
    private static volatile int currentVideoIndex = 0;
    
    private static JSlider thresholdSlider;
    private static JSlider frameCountSlider;
    private static JComboBox<String> videoSelector;
    private static JButton startPauseBtn;
    
    private static ScheduledExecutorService videoExecutor;
    private static ScheduledExecutorService processingExecutor;
    
    private static boolean firstInteraction = false;
    private static volatile boolean paramsChanged = false;

    private static List<String> paths = new ArrayList<>();
    private static List<VideoCapture> captures = new ArrayList<>();
    private static List<Boolean> isPlayingList = new ArrayList<>();
    private static List<Mat> currentFrames = new ArrayList<>();
    private static List<Mat> previousFrames = new ArrayList<>();
    private static List<List<Mat>> frameHistories = new ArrayList<>();
    private static List<BufferedImage> cachedOriginals = new ArrayList<>();
    private static List<BufferedImage> cachedAdjacents = new ArrayList<>();
    private static List<BufferedImage> cachedAverages = new ArrayList<>();
    private static List<Boolean> needsUpdates = new ArrayList<>();

    public static void main(String[] args) {
        videoExecutor = Executors.newScheduledThreadPool(1);
        processingExecutor = Executors.newScheduledThreadPool(2);
        
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Background subtraction");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(1400, 900);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                videoPanel = new JPanel();
                videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.Y_AXIS));
                videoPanel.setBackground(Color.WHITE);
                
                JScrollPane scrollPane = new JScrollPane(videoPanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                
                mainFrame.add(controlPanel, BorderLayout.NORTH);
                mainFrame.add(scrollPane, BorderLayout.CENTER);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
                startVideoProcessing();
            }
            catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Error: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void startVideoProcessing() {
        videoExecutor.scheduleAtFixedRate(() -> {
            try {
                boolean needRefresh = false;
                boolean updateUI = false;
                synchronized (videoLock) {
                    for (int i = 0; i < captures.size(); i++) {
                        if (isPlayingList.get(i)) {
                            boolean endReached = advanceFrame(i);

                            if (endReached) {
                                restart(i);
                                isPlayingList.set(i, false);

                                if (i == currentVideoIndex) {
                                    updateUI = true;
                                }
                                needRefresh = true;
                            }
                            else {
                                needRefresh = true;
                            }
                        }
                    }
                }
                if (needRefresh) {
                    refreshDisplay();
                }

                if (updateUI) {
                    SwingUtilities.invokeLater(() -> {
                        startPauseBtn.setText("Start");
                    });
                }
            }
            catch (Exception exception) {}
        }, 0, 40, TimeUnit.MILLISECONDS);
    }

    private static JPanel createControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel firstLinePanel = new JPanel();
        firstLinePanel.setBackground(mainPanel.getBackground());
        firstLinePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton openBtn = new JButton("Open file manager");
        JButton clearBtn = new JButton("Clear all");
        
        openBtn.addActionListener(event -> {
            firstInteraction = true;
            openFileManager();
        });
        clearBtn.addActionListener(event -> {
            firstInteraction = true;
            clearAll();
        });
        
        firstLinePanel.add(openBtn);
        firstLinePanel.add(clearBtn);
        
        JPanel secondLinePanel = new JPanel();
        secondLinePanel.setBackground(mainPanel.getBackground());
        secondLinePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        startPauseBtn = new JButton("Start");
        JButton restartBtn = new JButton("Restart");
        videoSelector = new JComboBox<>();
        videoSelector.setPreferredSize(new Dimension(150, 25));
        
        startPauseBtn.addActionListener(event -> {
            firstInteraction = true;

            if (captures.isEmpty()) {
                showNoVideosMessage();
            }
            else {
                toggleCurrentVideo();
            }
        });
        restartBtn.addActionListener(event -> {
            firstInteraction = true;

            if (captures.isEmpty()) {
                showNoVideosMessage();
            }
            else {
                restartCurrentVideo();
            }
        });
        videoSelector.addActionListener(event -> {
            firstInteraction = true;

            if (captures.isEmpty()) {
                showNoVideosMessage();
            }
            else {
                switchVideo();
            }
        });
        
        secondLinePanel.add(startPauseBtn);
        secondLinePanel.add(restartBtn);
        secondLinePanel.add(new JLabel("Select video:"));
        secondLinePanel.add(videoSelector);
        
        JPanel thresholdPanel = createControlRow("Threshold:", 0, 100, (int)(threshold * 100));
        JPanel frameCountPanel = createFrameCountControlRow();
        
        thresholdSlider = (JSlider) thresholdPanel.getClientProperty("slider");
        frameCountSlider = (JSlider) frameCountPanel.getClientProperty("slider");
        
        thresholdSlider.addChangeListener(event -> {
            firstInteraction = true;
            threshold = thresholdSlider.getValue() / 100.0;
            paramsChanged = true;

            if (!captures.isEmpty()) {
                refreshDisplay();
            }
            else {
                showNoVideosMessage();
            }
        });
        
        frameCountSlider.addChangeListener(event -> {
            firstInteraction = true;
            frameCount = frameCountSlider.getValue();
            paramsChanged = true;

            if (!captures.isEmpty()) {
                refreshDisplay();
            }
            else {
                showNoVideosMessage();
            }
        });
        
        mainPanel.add(firstLinePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(secondLinePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(thresholdPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(frameCountPanel);
        
        return mainPanel;
    }
    
    private static JPanel createControlRow(String label, int min, int max, int value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setMaximumSize(new Dimension(600, 60));
        
        JLabel textLabel = new JLabel(label);
        textLabel.setPreferredSize(new Dimension(150, 25));
        textLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        JSlider slider = new JSlider(min, max, value);
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setMinorTickSpacing((max - min) / 20);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(350, 50));
        
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        sliderPanel.setBackground(panel.getBackground());
        sliderPanel.add(slider);
        
        panel.add(textLabel, BorderLayout.WEST);
        panel.add(sliderPanel, BorderLayout.CENTER);
        
        panel.putClientProperty("slider", slider);
        
        return panel;
    }
    
    private static JPanel createFrameCountControlRow() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setMaximumSize(new Dimension(600, 60));
        
        JLabel textLabel = new JLabel("Frame count:");
        textLabel.setPreferredSize(new Dimension(150, 25));
        textLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        frameCountSlider = new JSlider(1, 20, frameCount);
        frameCountSlider.setMajorTickSpacing(5);
        frameCountSlider.setMinorTickSpacing(1);
        frameCountSlider.setPaintTicks(true);
        frameCountSlider.setPaintLabels(true);
        
        java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
        labelTable.put(1, new JLabel("1"));
        labelTable.put(5, new JLabel("5"));
        labelTable.put(10, new JLabel("10"));
        labelTable.put(15, new JLabel("15"));
        labelTable.put(20, new JLabel("20"));
        frameCountSlider.setLabelTable(labelTable);
        
        frameCountSlider.setPreferredSize(new Dimension(350, 50));
        
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        sliderPanel.setBackground(panel.getBackground());
        sliderPanel.add(frameCountSlider);
        
        panel.add(textLabel, BorderLayout.WEST);
        panel.add(sliderPanel, BorderLayout.CENTER);
        
        panel.putClientProperty("slider", frameCountSlider);
        
        return panel;
    }

    private static void openFileManager() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(selectedPaths -> {
            currentFileManager.close();
            currentFileManager = null;

            if (selectedPaths != null && selectedPaths.length > 0) {
                loadVideos(selectedPaths);
                updateVideoSelector();
                refreshDisplay();
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadVideos(String[] selectedPaths) {
        synchronized (videoLock) {
            for (String path : selectedPaths) {
                try {
                    File file = new File(path);

                    if (file.exists() && isVideoFile(file)) {
                        VideoCapture capture = new VideoCapture(path);

                        if (capture.isOpened()) {
                            paths.add(path);
                            captures.add(capture);
                            isPlayingList.add(false);
                            currentFrames.add(null);
                            previousFrames.add(null);
                            frameHistories.add(new ArrayList<>());
                            cachedOriginals.add(null);
                            cachedAdjacents.add(null);
                            cachedAverages.add(null);
                            needsUpdates.add(true);
                            loadInitialFrames(captures.size() - 1);
                        }
                        else {
                            capture.release();
                        }
                    }
                }
                catch (Exception exception) {}
            }
        }
    }
    
    private static void updateVideoSelector() {
        SwingUtilities.invokeLater(() -> {
            videoSelector.removeAllItems();

            for (int i = 0; i < captures.size(); i++) {
                videoSelector.addItem("Video " + (i + 1));
            }

            if (!captures.isEmpty()) {
                videoSelector.setEnabled(true);

                if (currentVideoIndex >= captures.size()) {
                    currentVideoIndex = 0;
                }

                videoSelector.setSelectedIndex(currentVideoIndex);
            }
            else {
                videoSelector.setEnabled(false);
                currentVideoIndex = 0;
            }
        });
    }
    
    private static boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();

        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".mkv") || name.endsWith(".wmv") || name.endsWith(".flv");
    }

    private static void toggleCurrentVideo() {
        synchronized (videoLock) {
            if (!captures.isEmpty() && currentVideoIndex < captures.size()) {
                boolean isPlaying = isPlayingList.get(currentVideoIndex);
                isPlayingList.set(currentVideoIndex, !isPlaying);
                startPauseBtn.setText(!isPlaying ? "Pause" : "Start");
            }
        }
    }

    private static void restartCurrentVideo() {
        synchronized (videoLock) {
            if (!captures.isEmpty() && currentVideoIndex < captures.size()) {
                restart(currentVideoIndex);
                isPlayingList.set(currentVideoIndex, false);
                startPauseBtn.setText("Start");
                refreshDisplay();
            }
        }
    }

    private static void switchVideo() {
        synchronized (videoLock) {
            int selectedIndex = videoSelector.getSelectedIndex();

            if (selectedIndex >= 0 && selectedIndex < captures.size()) {
                if (currentVideoIndex < captures.size()) {
                    isPlayingList.set(currentVideoIndex, false);
                }

                currentVideoIndex = selectedIndex;
                startPauseBtn.setText(isPlayingList.get(currentVideoIndex) ? "Pause" : "Start");
                refreshDisplay();
            }
        }
    }

    private static void refreshDisplay() {
        if (captures.isEmpty()) {
            if (firstInteraction) {
                showNoVideosMessage();
            }

            return;
        }
        
        processingExecutor.execute(() -> {
            try {
                final List<List<BufferedImage>> rowDataList = new ArrayList<>();
                
                synchronized (videoLock) {
                    for (int i = 0; i < captures.size(); i++) {
                        BufferedImage originalFrame;
                        BufferedImage adjacentResult;
                        BufferedImage averageResult;

                        if (paramsChanged || needsUpdates.get(i)) {
                            originalFrame = getCurrentFrame(i);
                            int width = originalFrame.getWidth();
                            int height = originalFrame.getHeight();
                            adjacentResult = applyAdjacentFrames(i, width, height);
                            averageResult = applyAverageFrames(i, width, height);
                            cachedOriginals.set(i, originalFrame);
                            cachedAdjacents.set(i, adjacentResult);
                            cachedAverages.set(i, averageResult);
                            needsUpdates.set(i, false);
                        }
                        else {
                            originalFrame = cachedOriginals.get(i);
                            adjacentResult = cachedAdjacents.get(i);
                            averageResult = cachedAverages.get(i);
                        }

                        rowDataList.add(List.of(originalFrame, adjacentResult, averageResult));
                    }
                    paramsChanged = false;
                }
                
                SwingUtilities.invokeLater(() -> {
                    try {
                        videoPanel.removeAll();
                        
                        for (List<BufferedImage> rowImages : rowDataList) {
                            JPanel videoRowPanel = createVideoRow(rowImages);
                            videoPanel.add(videoRowPanel);
                            videoPanel.add(Box.createRigidArea(new Dimension(0, 20)));
                        }
                        
                        videoPanel.revalidate();
                        videoPanel.repaint();
                    }
                    catch (Exception exception) {}
                });
            }
            catch (Exception exception) {}
        });
    }

    private static JPanel createVideoRow(List<BufferedImage> images) {
        JPanel videoRowPanel = new JPanel(new BorderLayout());
        videoRowPanel.setBackground(Color.WHITE);
        videoRowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel comparisonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        comparisonPanel.setBackground(Color.WHITE);
        
        addImageToPanel(comparisonPanel, images.get(0), "Original video");
        addImageToPanel(comparisonPanel, images.get(1), "Adjacent frames");
        addImageToPanel(comparisonPanel, images.get(2), "Average frames");
        
        videoRowPanel.add(comparisonPanel, BorderLayout.CENTER);

        return videoRowPanel;
    }
    
    private static BufferedImage applyAdjacentFrames(int index, int width, int height) {
        try {
            Mat currentFrame = currentFrames.get(index);
            Mat previousFrame = previousFrames.get(index);

            if (currentFrame == null || previousFrame == null) {
                return createBlackImage(width, height);
            }
            
            Mat diff = new Mat();
            Core.absdiff(currentFrame, previousFrame, diff);
            
            Mat gray = new Mat();
            Imgproc.cvtColor(diff, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat binary = new Mat();
            Imgproc.threshold(gray, binary, threshold * 255, 255, Imgproc.THRESH_BINARY);
            
            BufferedImage result = matToBufferedImage(binary);
            
            diff.release();
            gray.release();
            binary.release();
            
            return result;
        }
        catch (Exception exception) {
            return createBlackImage(width, height);
        }
    }
    
    private static BufferedImage applyAverageFrames(int index, int width, int height) {
        try {
            Mat currentFrame = currentFrames.get(index);

            if (currentFrame == null) {
                return createBlackImage(width, height);
            }
            
            List<Mat> frameHistory = frameHistories.get(index);

            if (frameHistory.size() < 2 || frameCount < 2) {
                return createBlackImage(width, height);
            }
            
            int framesToUse = Math.min(frameCount, frameHistory.size());
            
            if (framesToUse < 2) {
                return createBlackImage(width, height);
            }
            
            Mat averageFrame = new Mat(currentFrame.size(), CvType.CV_32FC3);
            averageFrame.setTo(new Scalar(0, 0, 0));
            
            int startIndex = Math.max(0, frameHistory.size() - framesToUse);
            int framesUsed = 0;
            
            for (int i = startIndex; i < frameHistory.size(); i++) {
                Mat frame = frameHistory.get(i);

                if (!frame.empty()) {
                    Mat floatFrame = new Mat();
                    frame.convertTo(floatFrame, CvType.CV_32FC3);
                    Core.add(averageFrame, floatFrame, averageFrame);
                    floatFrame.release();
                    framesUsed++;
                }
            }
            
            if (framesUsed == 0) {
                averageFrame.release();

                return createBlackImage(width, height);
            }
            
            Core.divide(averageFrame, new Scalar(framesUsed), averageFrame);
            
            Mat averageFrame8u = new Mat();
            averageFrame.convertTo(averageFrame8u, CvType.CV_8UC3);
            
            Mat diff = new Mat();
            Core.absdiff(currentFrame, averageFrame8u, diff);
            
            Mat gray = new Mat();
            Imgproc.cvtColor(diff, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat binary = new Mat();
            Imgproc.threshold(gray, binary, threshold * 255, 255, Imgproc.THRESH_BINARY);
            Core.bitwise_not(binary, binary);
            
            BufferedImage result = matToBufferedImage(binary);
            
            averageFrame.release();
            averageFrame8u.release();
            diff.release();
            gray.release();
            binary.release();
            
            return result;
            
        }
        catch (Exception exception) {
            return createBlackImage(width, height);
        }
    }
    
    private static BufferedImage createBlackImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        return image;
    }
    
    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 2), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        BufferedImage scaledImage = scaleImageForDisplay(image, 350);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        
        container.add(titleLabel, BorderLayout.NORTH);
        container.add(imageLabel, BorderLayout.CENTER);
        parent.add(container);
    }

    private static BufferedImage scaleImageForDisplay(BufferedImage original, int maxWidth) {
        if (original == null) {
            return createBlackImage(640, 480);
        }
        
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= maxWidth) {
            return original;
        }

        double scaleFactor = (double) maxWidth / originalWidth;
        int scaledWidth = maxWidth;
        int scaledHeight = (int) (originalHeight * scaleFactor);
        
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaled;
    }
    
    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            if (mat.empty()) {
                return createBlackImage(640, 480);
            }
            
            int type = BufferedImage.TYPE_BYTE_GRAY;

            if (mat.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }

            int bufferSize = mat.channels() * mat.cols() * mat.rows();
            byte[] buffer = new byte[bufferSize];
            mat.get(0, 0, buffer);
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            final byte[] targetPixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);

            return image;
        }
        catch (Exception exception) {
            return createBlackImage(640, 480);
        }
    }

    private static void clearAll() {
        synchronized (videoLock) {
            for (int i = 0; i < captures.size(); i++) {
                captures.get(i).release();

                if (currentFrames.get(i) != null) {
                    currentFrames.get(i).release();
                }

                if (previousFrames.get(i) != null) {
                    previousFrames.get(i).release();
                }

                for (Mat frame : frameHistories.get(i)) {
                    frame.release();
                }
            }

            paths.clear();
            captures.clear();
            isPlayingList.clear();
            currentFrames.clear();
            previousFrames.clear();
            frameHistories.clear();
            cachedOriginals.clear();
            cachedAdjacents.clear();
            cachedAverages.clear();
            needsUpdates.clear();
            currentVideoIndex = 0;
            updateVideoSelector();
            startPauseBtn.setText("Start");

            if (firstInteraction) {
                showNoVideosMessage();
            }
        }
    }

    private static void showNoVideosMessage() {
        SwingUtilities.invokeLater(() -> {
            videoPanel.removeAll();
            addLabel("<html>No videos loaded. Click &laquo;Open file manager&raquo; to load videos.</html>");
            videoPanel.revalidate();
            videoPanel.repaint();
        });
    }

    private static void addLabel(String text) {
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        labelPanel.setBackground(Color.WHITE);
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setBorder(BorderFactory.createEmptyBorder(50, 10, 50, 10));
        labelPanel.add(label);
        videoPanel.add(labelPanel);
    }
    
    private static void loadInitialFrames(int index) {
        try {
            VideoCapture capture = captures.get(index);
            capture.set(0, 0);
            
            Mat frame1 = new Mat();

            if (capture.read(frame1) && !frame1.empty()) {
                Mat current = frame1.clone();
                currentFrames.set(index, current);
                frameHistories.get(index).add(current.clone());
            }
            
            Mat frame2 = new Mat();

            if (capture.read(frame2) && !frame2.empty()) {
                Mat previous = currentFrames.get(index);
                previousFrames.set(index, previous);
                Mat current = frame2.clone();
                currentFrames.set(index, current);
                frameHistories.get(index).add(current.clone());
            }
            else if (currentFrames.get(index) != null) {
                Mat previous = currentFrames.get(index).clone();
                previousFrames.set(index, previous);
                frameHistories.get(index).add(currentFrames.get(index).clone());
            }
            
            frame1.release();
            frame2.release();
            
            needsUpdates.set(index, true);
        }
        catch (Exception exception) {}
    }
    
    private static boolean advanceFrame(int index) {
        try {
            VideoCapture capture = captures.get(index);

            if (capture.isOpened()) {
                Mat newFrame = new Mat();
                boolean readSuccess = capture.read(newFrame);
                
                if (readSuccess && !newFrame.empty()) {
                    Mat previous = previousFrames.get(index);

                    if (previous != null) {
                        previous.release();
                    }

                    previousFrames.set(index, currentFrames.get(index));
                    currentFrames.set(index, newFrame.clone());
                    
                    frameHistories.get(index).add(currentFrames.get(index).clone());

                    if (frameHistories.get(index).size() > 20) {
                        Mat removed = frameHistories.get(index).remove(0);
                        removed.release();
                    }

                    newFrame.release();
                    needsUpdates.set(index, true);

                    return false;
                }
                else {
                    newFrame.release();

                    return true; 
                }
            }
        }
        catch (Exception exception) {}

        return false;
    }
    
    private static BufferedImage getCurrentFrame(int index) {
        try {
            Mat current = currentFrames.get(index);

            if (current != null && !current.empty()) {
                return matToBufferedImage(current);
            }
        }
        catch (Exception exception) {}

        List<Mat> frameHistory = frameHistories.get(index);

        if (!frameHistory.isEmpty()) {
            Mat firstFrame = frameHistory.get(0);

            return createBlackImage(firstFrame.cols(), firstFrame.rows());
        }

        return createBlackImage(640, 480);
    }
    
    private static void restart(int index) {
        try {
            isPlayingList.set(index, false);
            
            Mat current = currentFrames.get(index);

            if (current != null) {
                current.release();
                currentFrames.set(index, null);
            }

            Mat previous = previousFrames.get(index);

            if (previous != null) {
                previous.release();
                previousFrames.set(index, null);
            }

            for (Mat frame : frameHistories.get(index)) {
                frame.release();
            }

            frameHistories.get(index).clear();
            
            VideoCapture capture = captures.get(index);

            if (capture != null) {
                capture.release();
            }

            capture = new VideoCapture(paths.get(index));
            captures.set(index, capture);
            
            loadInitialFrames(index);
            
            needsUpdates.set(index, true);
        }
        catch (Exception exception) {}
    }
}
