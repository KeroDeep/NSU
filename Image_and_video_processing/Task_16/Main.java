import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel videoPanel;
    private static final Object videoLock = new Object();
    private static FileManager currentFileManager = null;
    
    private static volatile int currentVideoIndex = 0;
    
    private static JButton startPauseBtn;
    private static JComboBox<String> videoSelector;
    
    private static ScheduledExecutorService videoExecutor;
    private static ScheduledExecutorService processingExecutor;
    
    private static boolean firstInteraction = false;

    private static List<String> paths = new ArrayList<>();
    private static List<VideoCapture> captures = new ArrayList<>();
    private static List<Boolean> isPlayingList = new ArrayList<>();
    private static List<Mat> currentFrames = new ArrayList<>();
    private static List<BufferedImage> cachedOriginals = new ArrayList<>();
    private static List<BufferedImage> cachedDetection = new ArrayList<>();
    private static List<Boolean> needsUpdates = new ArrayList<>();
    private static HOGDescriptor hog;
    private static List<Long> lastProcessedTimes = new ArrayList<>();
    private static final long PROCESSING_INTERVAL_MS = 200;

    public static void main(String[] args) {
        videoExecutor = Executors.newScheduledThreadPool(2);
        processingExecutor = Executors.newScheduledThreadPool(2);
        
        hog = new HOGDescriptor();
        hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
        
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Pedestrian detection");
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
        }, 0, 33, TimeUnit.MILLISECONDS);
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
        
        mainPanel.add(firstLinePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(secondLinePanel);
        
        return mainPanel;
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
                            cachedOriginals.add(null);
                            cachedDetection.add(null);
                            needsUpdates.add(true);
                            lastProcessedTimes.add(0L);
                            loadInitialFrame(captures.size() - 1);
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
                        BufferedImage detectionResult;

                        if (needsUpdates.get(i)) {
                            originalFrame = getCurrentFrame(i);
                            long currentTime = System.currentTimeMillis();
                            
                            if (currentTime - lastProcessedTimes.get(i) > PROCESSING_INTERVAL_MS) {
                                int width = originalFrame.getWidth();
                                int height = originalFrame.getHeight();
                                detectionResult = applyPedestrianDetection(i, width, height);
                                cachedDetection.set(i, detectionResult);
                                lastProcessedTimes.set(i, currentTime);
                            }
                            else {
                                detectionResult = cachedDetection.get(i);

                                if (detectionResult == null) {
                                    detectionResult = originalFrame;
                                }
                            }
                            
                            cachedOriginals.set(i, originalFrame);
                            needsUpdates.set(i, false);
                        }
                        else {
                            originalFrame = cachedOriginals.get(i);
                            detectionResult = cachedDetection.get(i);
                        }

                        rowDataList.add(List.of(originalFrame, detectionResult));
                    }
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
        
        JPanel comparisonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        comparisonPanel.setBackground(Color.WHITE);
        
        addImageToPanel(comparisonPanel, images.get(0), "Original video");
        addImageToPanel(comparisonPanel, images.get(1), "Pedestrian detection");
        
        videoRowPanel.add(comparisonPanel, BorderLayout.CENTER);

        return videoRowPanel;
    }
    
    private static BufferedImage applyPedestrianDetection(int index, int width, int height) {
        try {
            Mat detectionFrame = currentFrames.get(index).clone();
            Mat resizedFrame = new Mat();
            
            double scale = 0.5;

            if (detectionFrame.width() > 800) {
                Size newSize = new Size(detectionFrame.width() * scale, detectionFrame.height() * scale);
                Imgproc.resize(detectionFrame, resizedFrame, newSize);
            }
            else {
                resizedFrame = detectionFrame;
            }
            
            MatOfRect foundLocations = new MatOfRect();
            MatOfDouble foundWeights = new MatOfDouble();
            
            hog.detectMultiScale(resizedFrame, foundLocations, foundWeights, 0, new Size(8,8), new Size(32,32), 1.05, 2, false);
            
            Rect[] rects = foundLocations.toArray();
            double[] weights = foundWeights.toArray();
            
            for (int i = 0; i < rects.length; i++) {
                if (weights[i] > 0.5) {
                    Rect rect = rects[i];

                    if (scale != 1.0) {
                        rect.x = (int)(rect.x / scale);
                        rect.y = (int)(rect.y / scale);
                        rect.width = (int)(rect.width / scale);
                        rect.height = (int)(rect.height / scale);
                    }

                    Imgproc.rectangle(detectionFrame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
                    Imgproc.putText(detectionFrame, String.format("%.2f", weights[i]), new org.opencv.core.Point(rect.x, rect.y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 1);
                }
            }
            
            BufferedImage result = matToBufferedImage(detectionFrame);
            
            detectionFrame.release();

            if (resizedFrame != detectionFrame) {
                resizedFrame.release();
            }

            foundLocations.release();
            foundWeights.release();
            
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
            
            Mat rgb = new Mat();
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);

            int bufferSize = rgb.channels() * rgb.cols() * rgb.rows();
            byte[] buffer = new byte[bufferSize];
            rgb.get(0, 0, buffer);
            BufferedImage image = new BufferedImage(rgb.cols(), rgb.rows(), BufferedImage.TYPE_3BYTE_BGR);
            final byte[] targetPixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);

            rgb.release();
            
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
            }
            paths.clear();
            captures.clear();
            isPlayingList.clear();
            currentFrames.clear();
            cachedOriginals.clear();
            cachedDetection.clear();
            needsUpdates.clear();
            lastProcessedTimes.clear();
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
    
    private static void loadInitialFrame(int index) {
        try {
            VideoCapture capture = captures.get(index);
            capture.set(0, 0);
            
            Mat frame = new Mat();

            if (capture.read(frame) && !frame.empty()) {
                currentFrames.set(index, frame.clone());
                needsUpdates.set(index, true);
            }
            
            frame.release();
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
                    if (currentFrames.get(index) != null) {
                        currentFrames.get(index).release();
                    }
                    currentFrames.set(index, newFrame.clone());
                    
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
            
            VideoCapture capture = captures.get(index);

            if (capture != null) {
                capture.release();
            }

            capture = new VideoCapture(paths.get(index));
            captures.set(index, capture);
            
            loadInitialFrame(index);
            
            needsUpdates.set(index, true);
            lastProcessedTimes.set(index, 0L);
        }
        catch (Exception exception) {}
    }
}
