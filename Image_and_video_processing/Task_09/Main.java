import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static List<BufferedImage> originalColorImages = new ArrayList<>();
    private static List<BufferedImage> originalGrayImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();
    private static FileManager currentFileManager = null;
    
    private static boolean linesMode = true;
    
    private static int rho = 1;
    private static int theta = 180;
    private static int thresholdLines = 100;
    private static int minLineLength = 50;
    private static int maxLineGap = 10;
    
    private static int accumulatorResolution = 1;
    private static int minCenterDistance = 50;
    private static int cannyThreshold = 100;
    private static int accumulatorThreshold = 50;
    private static int minRadius = 10;
    private static int maxRadius = 100;

    private static JSlider rhoSlider;
    private static JSlider thetaSlider;
    private static JSlider thresholdLinesSlider;
    private static JSlider minLineLengthSlider;
    private static JSlider maxLineGapSlider;
    
    private static JSlider accumulatorResolutionSlider;
    private static JSlider minCenterDistanceSlider;
    private static JSlider cannyThresholdSlider;
    private static JSlider accumulatorThresholdSlider;
    private static JSlider minRadiusSlider;
    private static JSlider maxRadiusSlider;
    
    private static JButton detectionModeButton;
    private static JButton openBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Line and circle detection");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(1600, 1000);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                imagePanel = new JPanel();
                imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
                imagePanel.setBackground(Color.WHITE);
                
                JScrollPane scrollPane = new JScrollPane(imagePanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                
                mainFrame.add(controlPanel, BorderLayout.NORTH);
                mainFrame.add(scrollPane, BorderLayout.CENTER);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private static JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        openBtn = new JButton("Open file manager");
        JButton clearBtn = new JButton("Clear all");
        JButton resetBtn = new JButton("Reset parameters");
        detectionModeButton = new JButton("Detection: Lines");

        rhoSlider = new JSlider(1, 10, rho);
        rhoSlider.setMajorTickSpacing(2);
        rhoSlider.setMinorTickSpacing(1);
        rhoSlider.setPaintTicks(true);
        rhoSlider.setPaintLabels(true);

        thetaSlider = new JSlider(1, 360, theta);
        thetaSlider.setMajorTickSpacing(90);
        thetaSlider.setMinorTickSpacing(30);
        thetaSlider.setPaintTicks(true);
        thetaSlider.setPaintLabels(true);

        thresholdLinesSlider = new JSlider(0, 255, thresholdLines);
        thresholdLinesSlider.setMajorTickSpacing(50);
        thresholdLinesSlider.setMinorTickSpacing(10);
        thresholdLinesSlider.setPaintTicks(true);
        thresholdLinesSlider.setPaintLabels(true);

        minLineLengthSlider = new JSlider(0, 200, minLineLength);
        minLineLengthSlider.setMajorTickSpacing(50);
        minLineLengthSlider.setMinorTickSpacing(10);
        minLineLengthSlider.setPaintTicks(true);
        minLineLengthSlider.setPaintLabels(true);

        maxLineGapSlider = new JSlider(0, 50, maxLineGap);
        maxLineGapSlider.setMajorTickSpacing(10);
        maxLineGapSlider.setMinorTickSpacing(5);
        maxLineGapSlider.setPaintTicks(true);
        maxLineGapSlider.setPaintLabels(true);

        accumulatorResolutionSlider = new JSlider(1, 5, accumulatorResolution);
        accumulatorResolutionSlider.setMajorTickSpacing(1);
        accumulatorResolutionSlider.setMinorTickSpacing(1);
        accumulatorResolutionSlider.setPaintTicks(true);
        accumulatorResolutionSlider.setPaintLabels(true);

        minCenterDistanceSlider = new JSlider(0, 100, minCenterDistance);
        minCenterDistanceSlider.setMajorTickSpacing(20);
        minCenterDistanceSlider.setMinorTickSpacing(5);
        minCenterDistanceSlider.setPaintTicks(true);
        minCenterDistanceSlider.setPaintLabels(true);

        cannyThresholdSlider = new JSlider(0, 255, cannyThreshold);
        cannyThresholdSlider.setMajorTickSpacing(50);
        cannyThresholdSlider.setMinorTickSpacing(10);
        cannyThresholdSlider.setPaintTicks(true);
        cannyThresholdSlider.setPaintLabels(true);

        accumulatorThresholdSlider = new JSlider(0, 255, accumulatorThreshold);
        accumulatorThresholdSlider.setMajorTickSpacing(50);
        accumulatorThresholdSlider.setMinorTickSpacing(10);
        accumulatorThresholdSlider.setPaintTicks(true);
        accumulatorThresholdSlider.setPaintLabels(true);

        minRadiusSlider = new JSlider(0, 200, minRadius);
        minRadiusSlider.setMajorTickSpacing(50);
        minRadiusSlider.setMinorTickSpacing(10);
        minRadiusSlider.setPaintTicks(true);
        minRadiusSlider.setPaintLabels(true);

        maxRadiusSlider = new JSlider(0, 500, maxRadius);
        maxRadiusSlider.setMajorTickSpacing(100);
        maxRadiusSlider.setMinorTickSpacing(20);
        maxRadiusSlider.setPaintTicks(true);
        maxRadiusSlider.setPaintLabels(true);

        rhoSlider.addChangeListener(event -> {
            rho = rhoSlider.getValue();
            refreshDisplay();
        });

        thetaSlider.addChangeListener(event -> {
            theta = thetaSlider.getValue();
            refreshDisplay();
        });

        thresholdLinesSlider.addChangeListener(event -> {
            thresholdLines = thresholdLinesSlider.getValue();
            refreshDisplay();
        });

        minLineLengthSlider.addChangeListener(event -> {
            minLineLength = minLineLengthSlider.getValue();
            refreshDisplay();
        });

        maxLineGapSlider.addChangeListener(event -> {
            maxLineGap = maxLineGapSlider.getValue();
            refreshDisplay();
        });

        accumulatorResolutionSlider.addChangeListener(event -> {
            accumulatorResolution = accumulatorResolutionSlider.getValue();
            refreshDisplay();
        });

        minCenterDistanceSlider.addChangeListener(event -> {
            minCenterDistance = minCenterDistanceSlider.getValue();
            refreshDisplay();
        });

        cannyThresholdSlider.addChangeListener(event -> {
            cannyThreshold = cannyThresholdSlider.getValue();
            refreshDisplay();
        });

        accumulatorThresholdSlider.addChangeListener(event -> {
            accumulatorThreshold = accumulatorThresholdSlider.getValue();
            refreshDisplay();
        });

        minRadiusSlider.addChangeListener(event -> {
            minRadius = minRadiusSlider.getValue();

            if (minRadius >= maxRadius) {
                maxRadius = Math.min(500, minRadius + 10);
                maxRadiusSlider.setValue(maxRadius);
            }

            refreshDisplay();
        });

        maxRadiusSlider.addChangeListener(event -> {
            maxRadius = maxRadiusSlider.getValue();

            if (maxRadius <= minRadius) {
                minRadius = Math.max(0, maxRadius - 10);
                minRadiusSlider.setValue(minRadius);
            }

            refreshDisplay();
        });

        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        resetBtn.addActionListener(event -> resetParameters());
        detectionModeButton.addActionListener(event -> toggleDetectionMode());

        JPanel[] linePanels = createLabeledPanels("Lines", new String[]{"Distance resolution:", "Angle resolution:", "Line detection threshold:", "Minimum line length:", "Maximum line gap:"}, new JSlider[]{rhoSlider, thetaSlider, thresholdLinesSlider, minLineLengthSlider, maxLineGapSlider});

        JPanel[] circlePanels = createLabeledPanels("Circles", new String[]{"Accumulator resolution:", "Minimum center distance:", "Canny edge threshold:", "Accumulator threshold:", "Minimum circle radius:", "Maximum circle radius:"}, new JSlider[]{accumulatorResolutionSlider, minCenterDistanceSlider, cannyThresholdSlider, accumulatorThresholdSlider, minRadiusSlider, maxRadiusSlider});
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(openBtn, gbc);
        
        gbc.gridx = 1;
        panel.add(clearBtn, gbc);
        
        gbc.gridx = 2;
        panel.add(resetBtn, gbc);
        
        gbc.gridx = 3;
        panel.add(detectionModeButton, gbc);

        for (int i = 0; i < linePanels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i + 1; gbc.gridwidth = 4;
            panel.add(linePanels[i], gbc);
        }

        for (int i = 0; i < circlePanels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i + linePanels.length + 1; gbc.gridwidth = 4;
            panel.add(circlePanels[i], gbc);
            circlePanels[i].setVisible(false);
        }
        
        return panel;
    }

    private static JPanel[] createLabeledPanels(String type, String[] labels, JSlider[] sliders) {
        JPanel[] panels = new JPanel[labels.length];

        for (int i = 0; i < labels.length; i++) {
            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            labelPanel.setPreferredSize(new Dimension(180, 25));
            labelPanel.add(new JLabel(labels[i]));

            JPanel sliderPanel = new JPanel(new BorderLayout());
            sliderPanel.add(labelPanel, BorderLayout.WEST);
            sliderPanel.add(sliders[i], BorderLayout.CENTER);
            panels[i] = sliderPanel;
        }

        return panels;
    }

    private static void toggleDetectionMode() {
        linesMode = !linesMode;

        if (linesMode) {
            detectionModeButton.setText("Detection: Lines");
        }
        else {
            detectionModeButton.setText("Detection: Circles");
        }

        setPanelVisibility();
        mainFrame.revalidate();
        refreshDisplay();
    }

    private static void setPanelVisibility() {
        Component[] components = ((JPanel)mainFrame.getContentPane().getComponent(0)).getComponents();
        
        rhoSlider.getParent().setVisible(linesMode);
        thetaSlider.getParent().setVisible(linesMode);
        thresholdLinesSlider.getParent().setVisible(linesMode);
        minLineLengthSlider.getParent().setVisible(linesMode);
        maxLineGapSlider.getParent().setVisible(linesMode);
        
        accumulatorResolutionSlider.getParent().setVisible(!linesMode);
        minCenterDistanceSlider.getParent().setVisible(!linesMode);
        cannyThresholdSlider.getParent().setVisible(!linesMode);
        accumulatorThresholdSlider.getParent().setVisible(!linesMode);
        minRadiusSlider.getParent().setVisible(!linesMode);
        maxRadiusSlider.getParent().setVisible(!linesMode);
    }

    private static void resetParameters() {
        if (linesMode) {
            rho = 1;
            theta = 180;
            thresholdLines = 100;
            minLineLength = 50;
            maxLineGap = 10;
            
            rhoSlider.setValue(rho);
            thetaSlider.setValue(theta);
            thresholdLinesSlider.setValue(thresholdLines);
            minLineLengthSlider.setValue(minLineLength);
            maxLineGapSlider.setValue(maxLineGap);
        }
        else {
            accumulatorResolution = 1;
            minCenterDistance = 50;
            cannyThreshold = 100;
            accumulatorThreshold = 50;
            minRadius = 10;
            maxRadius = 100;
            
            accumulatorResolutionSlider.setValue(accumulatorResolution);
            minCenterDistanceSlider.setValue(minCenterDistance);
            cannyThresholdSlider.setValue(cannyThreshold);
            accumulatorThresholdSlider.setValue(accumulatorThreshold);
            minRadiusSlider.setValue(minRadius);
            maxRadiusSlider.setValue(maxRadius);
        }
        
        refreshDisplay();
    }

    private static void openFileManager() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(paths -> {
            currentFileManager.close();
            currentFileManager = null;

            if (paths.length > 0) {
                loadImages(paths);
            }
            
            refreshDisplay();
        });
        currentFileManager.showFileManager();
    }

    private static void loadImages(String[] paths) {
        for (String path : paths) {
            try {
                if (!imagePaths.contains(path)) {
                    BufferedImage colorImage = ImageIO.read(new File(path));

                    if (colorImage != null) {
                        BufferedImage scaledImage = scaleImageForProcessing(colorImage, 400, 300);
                        BufferedImage grayImage = convertToGrayscale(scaledImage);
                        originalColorImages.add(scaledImage);
                        originalGrayImages.add(grayImage);
                        imagePaths.add(path);
                    }
                }
            }
            catch (Exception exception) {
                System.err.println("Error loading image: " + path);
                exception.printStackTrace();
            }
        }
    }

    private static BufferedImage scaleImageForProcessing(BufferedImage original, int maxWidth, int maxHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return original;
        }

        double scaleFactor = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int scaledWidth = (int) (originalWidth * scaleFactor);
        int scaledHeight = (int) (originalHeight * scaleFactor);
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaled;
    }

    private static BufferedImage convertToGrayscale(BufferedImage colorImage) {
        BufferedImage grayImage = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(colorImage, 0, 0, null);
        g.dispose();

        return grayImage;
    }

    private static void refreshDisplay() {
        imagePanel.removeAll();

        if (originalColorImages.isEmpty()) {
            addLabel("<html>No images loaded. Click &laquo;Open file manager&raquo; to load images.</html>");
        }
        else {
            displayResults();
        }

        imagePanel.revalidate();
        imagePanel.repaint();
    }

    private static void displayResults() {
        for (int i = 0; i < originalColorImages.size(); i++) {
            JPanel rowPanel = new JPanel(new GridLayout(1, 4, 5, 5));
            rowPanel.setBackground(Color.WHITE);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            
            BufferedImage colorOriginal = originalColorImages.get(i);
            BufferedImage grayOriginal = originalGrayImages.get(i);
            
            BufferedImage edges = createEdgeImage(grayOriginal);
            BufferedImage detectionResult = createDetectionResult(colorOriginal, grayOriginal);
            
            addImageToPanel(rowPanel, colorOriginal, "Original");
            addImageToPanel(rowPanel, grayOriginal, "Grayscale");
            addImageToPanel(rowPanel, edges, "Edge detection");
            addImageToPanel(rowPanel, detectionResult, linesMode ? "Hough lines" : "Hough circles");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
    }

    private static BufferedImage createEdgeImage(BufferedImage gray) {
        Mat grayMat = bufferedImageToGrayMat(gray);
        Mat blurred = new Mat();
        Imgproc.medianBlur(grayMat, blurred, 5);
        
        Mat edges = new Mat();
        Imgproc.Canny(blurred, edges, 50, 150, 3, false);
        
        BufferedImage result = matToBufferedImage(edges);
        grayMat.release();
        blurred.release();
        edges.release();
        
        return result;
    }

    private static BufferedImage createDetectionResult(BufferedImage original, BufferedImage gray) {
        Mat colorMat = bufferedImageToMat(original);
        Mat grayMat = bufferedImageToGrayMat(gray);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(grayMat, blurred, new org.opencv.core.Size(9, 9), 2, 2);
        
        if (linesMode) {
            Mat edges = new Mat();
            Imgproc.Canny(blurred, edges, 50, 150, 3, false);
            
            Mat lines = new Mat();
            Imgproc.HoughLinesP(edges, lines, rho, Math.PI / theta, thresholdLines, minLineLength, maxLineGap);
            
            for (int i = 0; i < lines.rows(); i++) {
                double[] line = lines.get(i, 0);
                Imgproc.line(colorMat, new Point(line[0], line[1]), new Point(line[2], line[3]), new Scalar(0, 0, 255), 2);
            }

            lines.release();
            edges.release();
        }
        else {
            Mat circles = new Mat();
            int rows = blurred.rows();
            int minDistValue = Math.max(minCenterDistance, rows / 4);
            
            Imgproc.HoughCircles(blurred, circles, Imgproc.HOUGH_GRADIENT, accumulatorResolution, minDistValue, cannyThreshold, accumulatorThreshold, minRadius, maxRadius);
            
            for (int i = 0; i < circles.cols(); i++) {
                double[] circle = circles.get(0, i);
                Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                int radius = (int) Math.round(circle[2]);
                Imgproc.circle(colorMat, center, 3, new Scalar(0, 0, 255), -1);
                Imgproc.circle(colorMat, center, radius, new Scalar(0, 255, 0), 3);
            }

            circles.release();
        }
        
        BufferedImage result = matToBufferedImage(colorMat);
        
        colorMat.release();
        grayMat.release();
        blurred.release();
        
        return result;
    }

    private static Mat bufferedImageToMat(BufferedImage image) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            byte[] imageInByte = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return Imgcodecs.imdecode(new MatOfByte(imageInByte), Imgcodecs.IMREAD_COLOR);
        }
        catch (Exception exception) {
            exception.printStackTrace();

            return null;
        }
    }

    private static Mat bufferedImageToGrayMat(BufferedImage image) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            byte[] imageInByte = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return Imgcodecs.imdecode(new MatOfByte(imageInByte), Imgcodecs.IMREAD_GRAYSCALE);
        }
        catch (Exception exception) {
            exception.printStackTrace();

            return null;
        }
    }
    
    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, mob);
            byte[] byteArray = mob.toArray();
            mob.release();
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
            BufferedImage image = ImageIO.read(bis);
            bis.close();

            return image;
        }
        catch (Exception exception) {
            exception.printStackTrace();

            return null;
        }
    }

    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JLabel imageLabel = new JLabel(new ImageIcon(scaleImage(image, 300, 250)));
        container.add(titleLabel, BorderLayout.NORTH);
        container.add(imageLabel, BorderLayout.CENTER);
        parent.add(container);
    }

    private static void clearAll() {
        originalColorImages.clear();
        originalGrayImages.clear();
        imagePaths.clear();
        refreshDisplay();
    }

    private static void addLabel(String text) {
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        labelPanel.setBackground(Color.WHITE);
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setBorder(BorderFactory.createEmptyBorder(50, 10, 50, 10));
        labelPanel.add(label);
        imagePanel.add(labelPanel);
    }

    private static Image scaleImage(BufferedImage original, int maxWidth, int maxHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        double scaleFactor = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int scaledWidth = (int) (originalWidth * scaleFactor);
        int scaledHeight = (int) (originalHeight * scaleFactor);
        
        return original.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }
}
