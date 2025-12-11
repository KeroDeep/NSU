import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Scanner;

public class Main {
    private Mat originalImage;
    private Mat grayscaleImage;
    private Mat globalThresholdImage;
    private Mat adaptiveThresholdImage;
    private JLabel originalLabel;
    private JLabel grayscaleLabel;
    private JLabel globalThresholdLabel;
    private JLabel adaptiveThresholdLabel;
    
    private int globalThreshold = 75;
    private int blockSize = 15;
    private int constant = 5;
    
    private String originalImagePath;
    private int saveAdaptiveCounter = 1;
    private int saveGlobalCounter = 1;
    
    private JSlider globalThresholdSlider;
    private JSlider blockSizeSlider;
    private JSlider constantSlider;
    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new Main();
    }
    
    public Main() {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.print("Enter the path to the image: ");
            originalImagePath = scanner.nextLine();
            
            originalImage = Imgcodecs.imread(originalImagePath);
            
            if (originalImage.empty()) {
                System.out.println("Error: Failed to load image!");
                return;
            }
            
            grayscaleImage = new Mat();
            if (originalImage.channels() == 3) {
                Imgproc.cvtColor(originalImage, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
            }
            else {
                grayscaleImage = originalImage.clone();
            }
            
            globalThresholdImage = new Mat();
            adaptiveThresholdImage = new Mat();
            
            createGUI();
        }
        catch (Exception error) {
            System.out.println("Error: " + error.getMessage());
        }
        finally {
            scanner.close();
        }
    }
    
    private void createGUI() {
        JFrame frame = new JFrame("Adaptive thresholding");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        frame.setSize(1200, 800);
        
        JPanel imagePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        originalLabel = new JLabel();
        originalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        grayscaleLabel = new JLabel();
        grayscaleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        globalThresholdLabel = new JLabel();
        globalThresholdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        adaptiveThresholdLabel = new JLabel();
        adaptiveThresholdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        imagePanel.add(createPanelWithTitle(originalLabel, "Original image"));
        imagePanel.add(createPanelWithTitle(grayscaleLabel, "Black and white image"));
        imagePanel.add(createPanelWithTitle(globalThresholdLabel, "Global threshold"));
        imagePanel.add(createPanelWithTitle(adaptiveThresholdLabel, "Adaptive threshold"));
        
        JPanel controlPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        globalThresholdSlider = new JSlider(0, 255, globalThreshold);
        JLabel thresholdValue = new JLabel(String.valueOf(globalThreshold));
        setupSlider(globalThresholdSlider, thresholdValue, "Global threshold:", controlPanel);
        
        blockSizeSlider = new JSlider(3, 101, blockSize);
        blockSizeSlider.setSnapToTicks(true);
        blockSizeSlider.setPaintTicks(true);
        blockSizeSlider.setMajorTickSpacing(20);
        JLabel blockSizeValue = new JLabel(String.valueOf(blockSize));
        setupSlider(blockSizeSlider, blockSizeValue, "Block size:", controlPanel);
        
        constantSlider = new JSlider(-50, 50, constant);
        JLabel constantValue = new JLabel(String.valueOf(constant));
        setupSlider(constantSlider, constantValue, "Threshold shift:", controlPanel);
        
        globalThresholdSlider.addChangeListener(event -> {
            globalThreshold = globalThresholdSlider.getValue();
            thresholdValue.setText(String.valueOf(globalThreshold));
            applyThresholds();
        });
        
        blockSizeSlider.addChangeListener(event -> {
            int value = blockSizeSlider.getValue();
            blockSize = (value % 2 == 0) ? value + 1 : value;
            blockSizeValue.setText(String.valueOf(blockSize));
            applyThresholds();
        });
        
        constantSlider.addChangeListener(event -> {
            constant = constantSlider.getValue();
            constantValue.setText(String.valueOf(constant));
            applyThresholds();
        });
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(event -> resetParameters());
        
        JButton saveAdaptiveButton = new JButton("Save adaptive");
        saveAdaptiveButton.addActionListener(event -> saveAdaptiveImage());
        
        JButton saveGlobalButton = new JButton("Save global");
        saveGlobalButton.addActionListener(event -> saveGlobalImage());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(resetButton);
        buttonPanel.add(saveAdaptiveButton);
        buttonPanel.add(saveGlobalButton);
        
        frame.add(imagePanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(buttonPanel, BorderLayout.NORTH);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        applyThresholds();
    }
    
    private void resetParameters() {
        globalThreshold = 75;
        blockSize = 15;
        constant = 5;
        
        globalThresholdSlider.setValue(globalThreshold);
        blockSizeSlider.setValue(blockSize);
        constantSlider.setValue(constant);
        
        applyThresholds();
    }
    
    private JPanel createPanelWithTitle(JComponent component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }
    
    private void setupSlider(JSlider slider, JLabel valueLabel, String title, JPanel panel) {
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(new JLabel(title), BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        
        JPanel valuePanel = new JPanel(new BorderLayout());
        valuePanel.add(new JLabel("Value:"), BorderLayout.WEST);
        valuePanel.add(valueLabel, BorderLayout.CENTER);
        
        panel.add(sliderPanel);
        panel.add(valuePanel);
    }
    
    private void updateAllDisplays() {
        updateImageLabel(originalLabel, originalImage, true);
        updateImageLabel(grayscaleLabel, grayscaleImage, false);
        updateImageLabel(globalThresholdLabel, globalThresholdImage, false);
        updateImageLabel(adaptiveThresholdLabel, adaptiveThresholdImage, false);
    }
    
    private void applyThresholds() {
        if (grayscaleImage.empty()) {
            return;
        }
        
        Imgproc.threshold(grayscaleImage, globalThresholdImage, globalThreshold, 255, Imgproc.THRESH_BINARY);
        
        Imgproc.adaptiveThreshold(
            grayscaleImage, 
            adaptiveThresholdImage, 
            255, 
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 
            Imgproc.THRESH_BINARY, 
            blockSize, 
            constant
        );
        
        if (globalThresholdLabel != null && globalThresholdLabel.getParent() != null) {
            JPanel globalThresholdPanel = (JPanel) globalThresholdLabel.getParent();
            ((javax.swing.border.TitledBorder) globalThresholdPanel.getBorder()).setTitle("Global threshold (T = " + globalThreshold + ")");
            globalThresholdPanel.repaint();
        }
        
        updateAllDisplays();
    }
    
    private void updateImageLabel(JLabel label, Mat image, boolean convertBGRtoRGB) {
        if (image.empty()) {
            return;
        }
        
        Mat displayImage = new Mat();
        
        if (convertBGRtoRGB && image.channels() == 3) {
            Imgproc.cvtColor(image, displayImage, Imgproc.COLOR_BGR2RGB);
        }
        else {
            image.copyTo(displayImage);
        }
        
        BufferedImage bufferedImage = matToBufferedImage(displayImage);
        
        int displayWidth = 450;
        int displayHeight = 300;
        
        Image scaledImage = bufferedImage.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
        label.setIcon(new ImageIcon(scaledImage));
    }
    
    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat.empty()) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }
        
        Mat tempMat = new Mat();
        
        if (mat.depth() != CvType.CV_8U) {
            mat.convertTo(tempMat, CvType.CV_8U);
        }
        else {
            tempMat = mat;
        }
        
        int type = BufferedImage.TYPE_BYTE_GRAY;

        if (tempMat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        
        int bufferSize = tempMat.channels() * tempMat.cols() * tempMat.rows();
        byte[] buffer = new byte[bufferSize];
        tempMat.get(0, 0, buffer);
        
        BufferedImage image = new BufferedImage(tempMat.cols(), tempMat.rows(), type);
        image.getRaster().setDataElements(0, 0, tempMat.cols(), tempMat.rows(), buffer);
        
        return image;
    }
    
    private void saveAdaptiveImage() {
        saveImage(adaptiveThresholdImage, "adaptive", saveAdaptiveCounter);
        saveAdaptiveCounter++;
    }
    
    private void saveGlobalImage() {
        saveImage(globalThresholdImage, "global", saveGlobalCounter);
        saveGlobalCounter++;
    }
    
    private void saveImage(Mat image, String type, int counter) {
        if (originalImagePath == null || originalImagePath.isEmpty() || image.empty()) {
            JOptionPane.showMessageDialog(null, "No image loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            java.io.File originalFile = new java.io.File(originalImagePath);
            String originalName = originalFile.getName();
            String extension = "";
            String nameWithoutExtension = originalName;
            
            int dotIndex = originalName.lastIndexOf('.');

            if (dotIndex > 0) {
                extension = originalName.substring(dotIndex);
                nameWithoutExtension = originalName.substring(0, dotIndex);
            }
            
            String resultFileName = nameWithoutExtension + "_" + type + "_" + counter + extension;
            
            String resultPath;
            if (originalFile.getParent() != null) {
                resultPath = originalFile.getParent() + java.io.File.separator + resultFileName;
            }
            else {
                resultPath = resultFileName;
            }
            
            boolean success = Imgcodecs.imwrite(resultPath, image);
            if (success) {
                JOptionPane.showMessageDialog(null, type + " threshold image saved as: " + resultFileName, "Save", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                JOptionPane.showMessageDialog(null, "Error saving image", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception event) {
            JOptionPane.showMessageDialog(null, "Error saving image: " + event.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
