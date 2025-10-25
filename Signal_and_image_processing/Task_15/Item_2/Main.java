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
import org.opencv.video.Video;
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
    private static List<Mat> previousFrames = new ArrayList<>();
    private static List<BufferedImage> cachedOriginals = new ArrayList<>();
    private static List<BufferedImage> cachedFlow = new ArrayList<>();
    private static List<Boolean> needsUpdates = new ArrayList<>();
    private static List<Mat> previousGrays = new ArrayList<>();
    private static List<Mat> currentGrays = new ArrayList<>();
    private static List<MatOfPoint2f> trackedPtsList = new ArrayList<>();
    private static List<List<List<org.opencv.core.Point>>> pointsHistoriesList = new ArrayList<>();
    private static List<List<List<org.opencv.core.Point>>> trailHistoriesList = new ArrayList<>();

    public static void main(String[] args) {
        videoExecutor = Executors.newScheduledThreadPool(1);
        processingExecutor = Executors.newScheduledThreadPool(2);
        
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Optical flows");
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
                            previousFrames.add(null);
                            cachedOriginals.add(null);
                            cachedFlow.add(null);
                            needsUpdates.add(true);
                            previousGrays.add(null);
                            currentGrays.add(null);
                            trackedPtsList.add(new MatOfPoint2f());
                            pointsHistoriesList.add(new ArrayList<>());
                            trailHistoriesList.add(new ArrayList<>());
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
                        BufferedImage flowResult;

                        if (needsUpdates.get(i)) {
                            originalFrame = getCurrentFrame(i);
                            int width = originalFrame.getWidth();
                            int height = originalFrame.getHeight();
                            flowResult = applyOpticalFlow(i, width, height);
                            cachedOriginals.set(i, originalFrame);
                            cachedFlow.set(i, flowResult);
                            needsUpdates.set(i, false);
                        }
                        else {
                            originalFrame = cachedOriginals.get(i);
                            flowResult = cachedFlow.get(i);
                        }

                        rowDataList.add(List.of(originalFrame, flowResult));
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
        addImageToPanel(comparisonPanel, images.get(1), "Optical flow");
        
        videoRowPanel.add(comparisonPanel, BorderLayout.CENTER);

        return videoRowPanel;
    }
    
    private static BufferedImage applyOpticalFlow(int index, int width, int height) {
        try {
            Mat flowFrame = currentFrames.get(index).clone();
            
            List<List<org.opencv.core.Point>> activeHist = pointsHistoriesList.get(index);

            for (List<org.opencv.core.Point> hist : activeHist) {
                for (int k = 0; k < hist.size() - 1; k++) {
                    Imgproc.line(flowFrame, hist.get(k), hist.get(k+1), new Scalar(0, 255, 0), 2);
                }

                Imgproc.circle(flowFrame, hist.get(hist.size() - 1), 3, new Scalar(0, 0, 255), -1);
            }
            
            List<List<org.opencv.core.Point>> trails = trailHistoriesList.get(index);

            for (List<org.opencv.core.Point> trail : trails) {
                for (int k = 0; k < trail.size() - 1; k++) {
                    Imgproc.line(flowFrame, trail.get(k), trail.get(k+1), new Scalar(0, 255, 0), 2);
                }

                Imgproc.circle(flowFrame, trail.get(trail.size() - 1), 3, new Scalar(0, 0, 255), -1);
            }
            
            BufferedImage result = matToBufferedImage(flowFrame);
            
            flowFrame.release();
            
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

                if (previousGrays.get(i) != null) {
                    previousGrays.get(i).release();
                }

                if (currentGrays.get(i) != null) {
                    currentGrays.get(i).release();
                }
            }
            paths.clear();
            captures.clear();
            isPlayingList.clear();
            currentFrames.clear();
            previousFrames.clear();
            cachedOriginals.clear();
            cachedFlow.clear();
            needsUpdates.clear();
            previousGrays.clear();
            currentGrays.clear();
            trackedPtsList.clear();
            pointsHistoriesList.clear();
            trailHistoriesList.clear();
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
                currentFrames.set(index, frame1.clone());
                Mat gray1 = new Mat();
                Imgproc.cvtColor(frame1, gray1, Imgproc.COLOR_BGR2GRAY);
                currentGrays.set(index, gray1);
            }
            
            Mat frame2 = new Mat();

            if (capture.read(frame2) && !frame2.empty()) {
                previousFrames.set(index, currentFrames.get(index));
                currentFrames.set(index, frame2.clone());
                Mat gray2 = new Mat();
                Imgproc.cvtColor(frame2, gray2, Imgproc.COLOR_BGR2GRAY);
                previousGrays.set(index, currentGrays.get(index));
                currentGrays.set(index, gray2);
            }
            else if (currentFrames.get(index) != null) {
                previousFrames.set(index, currentFrames.get(index).clone());
                Mat gray = new Mat();
                Imgproc.cvtColor(currentFrames.get(index), gray, Imgproc.COLOR_BGR2GRAY);
                previousGrays.set(index, gray);
                currentGrays.set(index, gray.clone());
            }
            
            frame1.release();
            frame2.release();
            
            Mat currGray = currentGrays.get(index);

            if (currGray != null) {
                MatOfPoint corners = new MatOfPoint();
                Imgproc.goodFeaturesToTrack(currGray, corners, 1000, 0.0005, 5);
                trackedPtsList.set(index, new MatOfPoint2f(corners.toArray()));
                corners.release();
                
                org.opencv.core.Point[] initPts = trackedPtsList.get(index).toArray();
                List<List<org.opencv.core.Point>> initHist = new ArrayList<>();

                for (org.opencv.core.Point pt : initPts) {
                    List<org.opencv.core.Point> hist = new ArrayList<>();
                    hist.add(pt);
                    initHist.add(hist);
                }

                pointsHistoriesList.set(index, initHist);
            }
            
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
                    if (previousFrames.get(index) != null) {
                        previousFrames.get(index).release();
                    }
                    previousFrames.set(index, currentFrames.get(index));
                    currentFrames.set(index, newFrame.clone());
                    
                    Mat newGray = new Mat();
                    Imgproc.cvtColor(currentFrames.get(index), newGray, Imgproc.COLOR_BGR2GRAY);

                    if (previousGrays.get(index) != null) {
                        previousGrays.get(index).release();
                    }

                    previousGrays.set(index, currentGrays.get(index));
                    currentGrays.set(index, newGray);
                    
                    updateTrackedPoints(index);
                    
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
    
    private static void updateTrackedPoints(int index) {
        try {
            Mat prevGray = previousGrays.get(index);
            Mat currGray = currentGrays.get(index);
            MatOfPoint2f prevPts = trackedPtsList.get(index);

            if (prevPts.empty()) {
                return;
            }
            
            MatOfPoint2f nextPts = new MatOfPoint2f();
            MatOfByte status = new MatOfByte();
            MatOfFloat err = new MatOfFloat();
            Video.calcOpticalFlowPyrLK(prevGray, currGray, prevPts, nextPts, status, err);
            
            List<List<org.opencv.core.Point>> histories = pointsHistoriesList.get(index);
            List<List<org.opencv.core.Point>> newHist = new ArrayList<>();
            List<org.opencv.core.Point> newPtsList = new ArrayList<>();
            byte[] stArr = status.toArray();
            org.opencv.core.Point[] nextArr = nextPts.toArray();
            
            for (int j = 0; j < stArr.length; j++) {
                if (stArr[j] == 1) {
                    List<org.opencv.core.Point> hist = new ArrayList<>(histories.get(j));
                    hist.add(nextArr[j]);
                    newHist.add(hist);
                    newPtsList.add(nextArr[j]);
                }
                else {
                    trailHistoriesList.get(index).add(histories.get(j));
                }
            }
            
            pointsHistoriesList.set(index, newHist);
            
            trackedPtsList.set(index, new MatOfPoint2f(newPtsList.toArray(new org.opencv.core.Point[0])));
            
            if (newPtsList.size() < 200) {
                MatOfPoint newCorners = new MatOfPoint();
                Imgproc.goodFeaturesToTrack(currGray, newCorners, 1000 - newPtsList.size(), 0.0005, 5);

                if (!newCorners.empty()) {
                    org.opencv.core.Point[] newC = newCorners.toArray();

                    for (org.opencv.core.Point pt : newC) {
                        List<org.opencv.core.Point> newHistItem = new ArrayList<>();
                        newHistItem.add(pt);
                        newHist.add(newHistItem);
                        newPtsList.add(pt);
                    }

                    trackedPtsList.set(index, new MatOfPoint2f(newPtsList.toArray(new org.opencv.core.Point[0])));
                    pointsHistoriesList.set(index, newHist);
                }

                newCorners.release();
            }
        }
        catch (Exception exception) {}
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

            Mat previous = previousFrames.get(index);

            if (previous != null) {
                previous.release();
                previousFrames.set(index, null);
            }

            Mat prevGray = previousGrays.get(index);

            if (prevGray != null) {
                prevGray.release();
                previousGrays.set(index, null);
            }

            Mat currGray = currentGrays.get(index);

            if (currGray != null) {
                currGray.release();
                currentGrays.set(index, null);
            }
            
            trackedPtsList.set(index, new MatOfPoint2f());
            pointsHistoriesList.set(index, new ArrayList<>());
            trailHistoriesList.set(index, new ArrayList<>());
            
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
