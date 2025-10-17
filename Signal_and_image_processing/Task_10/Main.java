import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.ByteArrayInputStream;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static List<BufferedImage> originalImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();
    private static FileManager currentFileManager = null;
    
    private static double sensitivity = 0.04;
    private static int blockSize = 2;
    private static int aperture = 3;
    private static double threshold = 0.01;
    
    private static int maxCorners = 100;
    private static double qualityLevel = 0.01;
    private static double minDistance = 10;
    private static int shiTomasiBlockSize = 3;
    private static boolean useHarris = false;
    private static double shiTomasiSensitivity = 0.04;
    
    private static double rotationAngle = 15.0;
    private static double scaleFactor = 1.0;
    private static double shearAmount = 0.1;
    private static double perspectiveStrength = 0.02;
    private static double brightness = 30.0;
    private static double contrast = 1.3;
    
    private static JSlider sensitivitySlider;
    private static JSlider blockSizeSlider;
    private static JComboBox<String> apertureComboBox;
    private static JSlider thresholdSlider;
    
    private static JSlider maxCornersSlider;
    private static JSlider qualityLevelSlider;
    private static JSlider minDistanceSlider;
    private static JSlider shiTomasiBlockSizeSlider;
    private static JCheckBox useHarrisCheckBox;
    private static JSlider shiTomasiSensitivitySlider;
    
    private static JSlider rotationAngleSlider;
    private static JSlider scaleFactorSlider;
    private static JSlider shearAmountSlider;
    private static JSlider perspectiveStrengthSlider;
    private static JSlider brightnessSlider;
    private static JSlider contrastSlider;
    
    private static boolean isResetting = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Local feature detection");
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
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(mainPanel.getBackground());
        
        JButton openBtn = new JButton("Open file manager");
        JButton clearBtn = new JButton("Clear all");
        JButton resetBtn = new JButton("Reset parameters");
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        JPanel harrisPanel = createHarrisPanel();
        tabbedPane.addTab("Harris detector", harrisPanel);
        
        JPanel shiTomasiPanel = createShiTomasiPanel();
        tabbedPane.addTab("Shi-Tomasi detector", shiTomasiPanel);
        
        JPanel transformPanel = createTransformPanel();
        tabbedPane.addTab("Image transformations", transformPanel);
        
        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        resetBtn.addActionListener(event -> resetParameters());
        
        buttonPanel.add(openBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(resetBtn);
        
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(tabbedPane);
        
        return mainPanel;
    }
    
    private static JPanel createHarrisPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        sensitivitySlider = new JSlider(0, 100, (int)(sensitivity * 1000));
        sensitivitySlider.setMajorTickSpacing(20);
        sensitivitySlider.setMinorTickSpacing(5);
        sensitivitySlider.setPaintTicks(true);
        sensitivitySlider.setPaintLabels(true);
        
        blockSizeSlider = new JSlider(1, 10, blockSize);
        blockSizeSlider.setMajorTickSpacing(3);
        blockSizeSlider.setMinorTickSpacing(1);
        blockSizeSlider.setPaintTicks(true);
        blockSizeSlider.setPaintLabels(true);
        
        String[] apertureOptions = {"1x1", "3x3", "5x5", "7x7"};
        apertureComboBox = new JComboBox<>(apertureOptions);
        apertureComboBox.setSelectedIndex(getApertureIndex(aperture));
        
        thresholdSlider = new JSlider(0, 50, (int)(threshold * 1000));
        thresholdSlider.setMajorTickSpacing(10);
        thresholdSlider.setMinorTickSpacing(2);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        
        sensitivitySlider.addChangeListener(event -> {
            if (!isResetting) {
                sensitivity = sensitivitySlider.getValue() / 1000.0;
                refreshDisplay();
            }
        });
        
        blockSizeSlider.addChangeListener(event -> {
            if (!isResetting) {
                blockSize = blockSizeSlider.getValue();
                if (blockSize % 2 == 0) blockSize++;
                refreshDisplay();
            }
        });
        
        apertureComboBox.addActionListener(event -> {
            if (!isResetting) {
                aperture = getApertureValue(apertureComboBox.getSelectedIndex());
                refreshDisplay();
            }
        });
        
        thresholdSlider.addChangeListener(event -> {
            if (!isResetting) {
                threshold = thresholdSlider.getValue() / 1000.0;
                refreshDisplay();
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Sensitivity:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(sensitivitySlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Block size:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(blockSizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Aperture:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(apertureComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Threshold:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(thresholdSlider, gbc);
        
        return panel;
    }
    
    private static int getApertureIndex(int apertureValue) {
        switch (apertureValue) {
            case 1: return 0;
            case 3: return 1;
            case 5: return 2;
            case 7: return 3;
            default: return 1;
        }
    }
    
    private static int getApertureValue(int index) {
        switch (index) {
            case 0: return 1;
            case 1: return 3;
            case 2: return 5;
            case 3: return 7;
            default: return 3;
        }
    }
    
    private static JPanel createShiTomasiPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        maxCornersSlider = new JSlider(0, 1000, maxCorners);
        maxCornersSlider.setMajorTickSpacing(200);
        maxCornersSlider.setMinorTickSpacing(50);
        maxCornersSlider.setPaintTicks(true);
        maxCornersSlider.setPaintLabels(true);
        
        qualityLevelSlider = new JSlider(0, 100, (int)(qualityLevel * 1000));
        qualityLevelSlider.setMajorTickSpacing(20);
        qualityLevelSlider.setMinorTickSpacing(5);
        qualityLevelSlider.setPaintTicks(true);
        qualityLevelSlider.setPaintLabels(true);
        
        minDistanceSlider = new JSlider(0, 50, (int)minDistance);
        minDistanceSlider.setMajorTickSpacing(10);
        minDistanceSlider.setMinorTickSpacing(2);
        minDistanceSlider.setPaintTicks(true);
        minDistanceSlider.setPaintLabels(true);
        
        shiTomasiBlockSizeSlider = new JSlider(1, 10, shiTomasiBlockSize);
        shiTomasiBlockSizeSlider.setMajorTickSpacing(3);
        shiTomasiBlockSizeSlider.setMinorTickSpacing(1);
        shiTomasiBlockSizeSlider.setPaintTicks(true);
        shiTomasiBlockSizeSlider.setPaintLabels(true);
        
        useHarrisCheckBox = new JCheckBox("Harris detector", useHarris);
        
        shiTomasiSensitivitySlider = new JSlider(0, 100, (int)(shiTomasiSensitivity * 1000));
        shiTomasiSensitivitySlider.setMajorTickSpacing(20);
        shiTomasiSensitivitySlider.setMinorTickSpacing(5);
        shiTomasiSensitivitySlider.setPaintTicks(true);
        shiTomasiSensitivitySlider.setPaintLabels(true);
        
        maxCornersSlider.addChangeListener(event -> {
            if (!isResetting) {
                maxCorners = maxCornersSlider.getValue();
                refreshDisplay();
            }
        });
        
        qualityLevelSlider.addChangeListener(event -> {
            if (!isResetting) {
                qualityLevel = qualityLevelSlider.getValue() / 1000.0;
                refreshDisplay();
            }
        });
        
        minDistanceSlider.addChangeListener(event -> {
            if (!isResetting) {
                minDistance = minDistanceSlider.getValue();
                refreshDisplay();
            }
        });
        
        shiTomasiBlockSizeSlider.addChangeListener(event -> {
            if (!isResetting) {
                shiTomasiBlockSize = shiTomasiBlockSizeSlider.getValue();
                if (shiTomasiBlockSize % 2 == 0) shiTomasiBlockSize++;
                refreshDisplay();
            }
        });
        
        useHarrisCheckBox.addActionListener(event -> {
            if (!isResetting) {
                useHarris = useHarrisCheckBox.isSelected();
                refreshDisplay();
            }
        });
        
        shiTomasiSensitivitySlider.addChangeListener(event -> {
            if (!isResetting) {
                shiTomasiSensitivity = shiTomasiSensitivitySlider.getValue() / 1000.0;
                refreshDisplay();
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Max corners:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(maxCornersSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Quality level:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(qualityLevelSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Min distance:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(minDistanceSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Block size:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(shiTomasiBlockSizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(useHarrisCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        panel.add(new JLabel("Sensitivity:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(shiTomasiSensitivitySlider, gbc);
        
        return panel;
    }
    
    private static JPanel createTransformPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        rotationAngleSlider = new JSlider(-360, 360, (int)rotationAngle);
        rotationAngleSlider.setMajorTickSpacing(120);
        rotationAngleSlider.setMinorTickSpacing(30);
        rotationAngleSlider.setPaintTicks(true);
        rotationAngleSlider.setPaintLabels(true);
        
        scaleFactorSlider = new JSlider(50, 200, (int)(scaleFactor * 100));
        scaleFactorSlider.setMajorTickSpacing(50);
        scaleFactorSlider.setMinorTickSpacing(10);
        scaleFactorSlider.setPaintTicks(true);
        scaleFactorSlider.setPaintLabels(true);
        
        shearAmountSlider = new JSlider(-50, 50, (int)(shearAmount * 100));
        shearAmountSlider.setMajorTickSpacing(25);
        shearAmountSlider.setMinorTickSpacing(5);
        shearAmountSlider.setPaintTicks(true);
        shearAmountSlider.setPaintLabels(true);
        
        perspectiveStrengthSlider = new JSlider(-50, 50, (int)(perspectiveStrength * 100));
        perspectiveStrengthSlider.setMajorTickSpacing(25);
        perspectiveStrengthSlider.setMinorTickSpacing(5);
        perspectiveStrengthSlider.setPaintTicks(true);
        perspectiveStrengthSlider.setPaintLabels(true);
        
        brightnessSlider = new JSlider(-100, 100, (int)brightness);
        brightnessSlider.setMajorTickSpacing(50);
        brightnessSlider.setMinorTickSpacing(10);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
        
        contrastSlider = new JSlider(0, 300, (int)(contrast * 100));
        contrastSlider.setMajorTickSpacing(100);
        contrastSlider.setMinorTickSpacing(25);
        contrastSlider.setPaintTicks(true);
        contrastSlider.setPaintLabels(true);
        
        rotationAngleSlider.addChangeListener(event -> {
            if (!isResetting) {
                rotationAngle = rotationAngleSlider.getValue();
                refreshDisplay();
            }
        });
        
        scaleFactorSlider.addChangeListener(event -> {
            if (!isResetting) {
                scaleFactor = scaleFactorSlider.getValue() / 100.0;
                refreshDisplay();
            }
        });
        
        shearAmountSlider.addChangeListener(event -> {
            if (!isResetting) {
                shearAmount = shearAmountSlider.getValue() / 100.0;
                refreshDisplay();
            }
        });
        
        perspectiveStrengthSlider.addChangeListener(event -> {
            if (!isResetting) {
                perspectiveStrength = perspectiveStrengthSlider.getValue() / 100.0;
                refreshDisplay();
            }
        });
        
        brightnessSlider.addChangeListener(event -> {
            if (!isResetting) {
                brightness = brightnessSlider.getValue();
                refreshDisplay();
            }
        });
        
        contrastSlider.addChangeListener(event -> {
            if (!isResetting) {
                contrast = contrastSlider.getValue() / 100.0;
                refreshDisplay();
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Rotation angle:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(rotationAngleSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Scale factor:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(scaleFactorSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Shear amount:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(shearAmountSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Perspective strength:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(perspectiveStrengthSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        panel.add(new JLabel("Brightness:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(brightnessSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        panel.add(new JLabel("Contrast:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(contrastSlider, gbc);
        
        return panel;
    }

    private static void resetParameters() {
        isResetting = true;
        
        sensitivity = 0.04;
        blockSize = 3;
        aperture = 3;
        threshold = 0.01;
        
        maxCorners = 100;
        qualityLevel = 0.01;
        minDistance = 10;
        shiTomasiBlockSize = 3;
        useHarris = false;
        shiTomasiSensitivity = 0.04;
        
        rotationAngle = 15.0;
        scaleFactor = 1.0;
        shearAmount = 0.1;
        perspectiveStrength = 0.02;
        brightness = 30.0;
        contrast = 1.3;
        
        sensitivitySlider.setValue((int)(sensitivity * 1000));
        blockSizeSlider.setValue(blockSize);
        apertureComboBox.setSelectedIndex(getApertureIndex(aperture));
        thresholdSlider.setValue((int)(threshold * 1000));
        
        maxCornersSlider.setValue(maxCorners);
        qualityLevelSlider.setValue((int)(qualityLevel * 1000));
        minDistanceSlider.setValue((int)minDistance);
        shiTomasiBlockSizeSlider.setValue(shiTomasiBlockSize);
        useHarrisCheckBox.setSelected(useHarris);
        shiTomasiSensitivitySlider.setValue((int)(shiTomasiSensitivity * 1000));
        
        rotationAngleSlider.setValue((int)rotationAngle);
        scaleFactorSlider.setValue((int)(scaleFactor * 100));
        shearAmountSlider.setValue((int)(shearAmount * 100));
        perspectiveStrengthSlider.setValue((int)(perspectiveStrength * 100));
        brightnessSlider.setValue((int)brightness);
        contrastSlider.setValue((int)(contrast * 100));
        
        isResetting = false;
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

            if (paths != null && paths.length > 0) {
                loadImages(paths);
            }

            refreshDisplay();
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadImages(String[] paths) {
        for (String path : paths) {
            try {
                if (imagePaths.contains(path)) {
                    continue;
                }
                
                File file = new File(path);

                if (!file.exists()) {
                    continue;
                }
                
                BufferedImage image = ImageIO.read(file);

                if (image != null) {
                    BufferedImage scaledImage = scaleImageForDisplay(image, 400);
                    originalImages.add(scaledImage);
                    imagePaths.add(path);
                }
            }
            catch (Exception event) {}
        }
    }

    private static BufferedImage scaleImageForDisplay(BufferedImage original, int maxWidth) {
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

    private static void refreshDisplay() {
        imagePanel.removeAll();

        if (originalImages.isEmpty()) {
            addLabel("<html>No images loaded. Click &laquo;Open file manager&raquo; to load images.</html>");
        }
        else {
            displayResults();
        }

        imagePanel.revalidate();
        imagePanel.repaint();
    }

    private static void displayResults() {
        for (int i = 0; i < originalImages.size(); i++) {
            BufferedImage original = originalImages.get(i);
            
            JPanel imageRowPanel = new JPanel(new BorderLayout());
            imageRowPanel.setBackground(Color.WHITE);
            imageRowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            Mat originalMat = bufferedImageToMat(original);

            if (originalMat == null || originalMat.empty()) {
                continue;
            }
            
            Mat grayMat = new Mat();
            Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
            
            MatOfPoint harrisCorners = detectHarrisCorners(grayMat);
            MatOfPoint shiTomasiCorners = detectShiTomasiCorners(grayMat);
            
            BufferedImage transformedImage = applyTransformations(original);
            Mat transformedMat = bufferedImageToMat(transformedImage);
            Mat transformedGray = new Mat();
            Imgproc.cvtColor(transformedMat, transformedGray, Imgproc.COLOR_BGR2GRAY);
            
            MatOfPoint transformedHarrisCorners = detectHarrisCorners(transformedGray);
            MatOfPoint transformedShiTomasiCorners = detectShiTomasiCorners(transformedGray);
            
            BufferedImage harrisResult = drawCornersOnImage(original, harrisCorners, Color.RED);
            BufferedImage shiTomasiResult = drawCornersOnImage(original, shiTomasiCorners, Color.GREEN);
            BufferedImage transformedHarrisResult = drawCornersOnImage(transformedImage, transformedHarrisCorners, Color.RED);
            BufferedImage transformedShiTomasiResult = drawCornersOnImage(transformedImage, transformedShiTomasiCorners, Color.GREEN);
            
            JPanel comparisonPanel = new JPanel(new GridLayout(1, 5, 10, 10));
            comparisonPanel.setBackground(Color.WHITE);
            
            addImageToPanel(comparisonPanel, original, "Original");
            addImageToPanel(comparisonPanel, harrisResult, String.format("Harris (%d corners)", harrisCorners.rows()));
            addImageToPanel(comparisonPanel, shiTomasiResult, String.format("Shi-Tomasi (%d corners)", shiTomasiCorners.rows()));
            addImageToPanel(comparisonPanel, transformedHarrisResult, String.format("Transformed Harris (%d corners)", transformedHarrisCorners.rows()));
            addImageToPanel(comparisonPanel, transformedShiTomasiResult, String.format("Transformed Shi-Tomasi (%d corners)", transformedShiTomasiCorners.rows()));
            
            imageRowPanel.add(comparisonPanel, BorderLayout.CENTER);
            imagePanel.add(imageRowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 20)));
            
            originalMat.release();
            grayMat.release();
            harrisCorners.release();
            shiTomasiCorners.release();
            transformedMat.release();
            transformedGray.release();
            transformedHarrisCorners.release();
            transformedShiTomasiCorners.release();
        }
    }
    
    private static MatOfPoint detectHarrisCorners(Mat gray) {
        Mat dst = new Mat();
        Mat dstNorm = new Mat();
        Mat dstNormScaled = new Mat();
        
        Mat grayFloat = new Mat();
        gray.convertTo(grayFloat, CvType.CV_32F);
        
        Imgproc.cornerHarris(grayFloat, dst, blockSize, aperture, sensitivity);
        Core.normalize(dst, dstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(dstNorm, dstNormScaled);
        
        Mat binary = new Mat();
        Imgproc.threshold(dstNormScaled, binary, threshold * 255, 255, Imgproc.THRESH_BINARY);
        
        Mat corners = new Mat();
        Core.findNonZero(binary, corners);
        
        MatOfPoint result = new MatOfPoint();
        corners.convertTo(result, CvType.CV_32S);
        
        dst.release();
        dstNorm.release();
        dstNormScaled.release();
        binary.release();
        grayFloat.release();
        corners.release();
        
        return result;
    }
    
    private static MatOfPoint detectShiTomasiCorners(Mat gray) {
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(gray, corners, maxCorners, qualityLevel, minDistance, new Mat(), shiTomasiBlockSize, useHarris, shiTomasiSensitivity);

        return corners;
    }
    
    private static BufferedImage applyTransformations(BufferedImage original) {
        Mat originalMat = bufferedImageToMat(original);
        
        Mat transformed = originalMat.clone();
        
        if (rotationAngle != 0) {
            org.opencv.core.Point center = new org.opencv.core.Point(transformed.cols() / 2.0, transformed.rows() / 2.0);
            Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, rotationAngle, 1.0);
            Imgproc.warpAffine(transformed, transformed, rotationMatrix, transformed.size());
            rotationMatrix.release();
        }
        
        if (scaleFactor != 1.0) {
            Imgproc.resize(transformed, transformed, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR);
        }
        
        if (shearAmount != 0) {
            Mat shearMatrix = new Mat(2, 3, CvType.CV_64F);
            shearMatrix.put(0, 0, 1.0, shearAmount, 0.0);
            shearMatrix.put(1, 0, 0.0, 1.0, 0.0);
            Imgproc.warpAffine(transformed, transformed, shearMatrix, transformed.size());
            shearMatrix.release();
        }
        
        if (perspectiveStrength != 0) {
            Mat perspectiveMatrix = new Mat(3, 3, CvType.CV_64F);
            perspectiveMatrix.put(0, 0, 1.0, perspectiveStrength, 0.0);
            perspectiveMatrix.put(1, 0, perspectiveStrength, 1.0, 0.0);
            perspectiveMatrix.put(2, 0, 0.0, 0.0, 1.0);
            Imgproc.warpPerspective(transformed, transformed, perspectiveMatrix, transformed.size());
            perspectiveMatrix.release();
        }
        
        if (brightness != 0 || contrast != 1.0) {
            transformed.convertTo(transformed, -1, contrast, brightness);
        }
        
        BufferedImage result = matToBufferedImage(transformed);
        originalMat.release();
        transformed.release();
        
        return result;
    }
    
    private static BufferedImage drawCornersOnImage(BufferedImage original, MatOfPoint corners, Color color) {
        BufferedImage result = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        
        org.opencv.core.Point[] points = corners.toArray();

        for (org.opencv.core.Point point : points) {
            int x = (int) point.x;
            int y = (int) point.y;
            g2d.fillOval(x - 3, y - 3, 6, 6);
        }
        
        g2d.dispose();

        return result;
    }

    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        Image scaledImage = image.getScaledInstance(250, 188, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        container.add(titleLabel, BorderLayout.NORTH);
        container.add(imageLabel, BorderLayout.CENTER);
        parent.add(container);
    }

    private static Mat bufferedImageToMat(BufferedImage image) {
        try {
            BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = convertedImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            
            byte[] pixels = ((java.awt.image.DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
            mat.put(0, 0, pixels);
            
            return mat;
        }
        catch (Exception exception) {
            return new Mat();
        }
    }
    
    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, mob);
            byte[] byteArray = mob.toArray();
            mob.release();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
            BufferedImage image = ImageIO.read(bais);
            bais.close();
            
            return image;
        }
        catch (Exception exception) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }
    }

    private static void clearAll() {
        originalImages.clear();
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
}
