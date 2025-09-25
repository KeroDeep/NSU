import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Scanner;

public class Main {
    private static Mat originalImage;
    private static Mat equalizedImage;
    private static Mat correctedImage;
    private static JLabel originalLabel;
    private static JLabel equalizedLabel;
    private static JLabel correctedLabel;
    private static JLabel originalHistLabel;
    private static JLabel equalizedHistLabel;
    private static JLabel correctedHistLabel;
    
    private static double alpha = 1.0;
    private static double beta = 0.0;
    private static double gamma = 1.0;

    private static String originalImagePath;
    private static int saveCounter = 1;
    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.print("Enter the path to the image: ");
            originalImagePath = scanner.nextLine();
            
            originalImage = Imgcodecs.imread(originalImagePath);
            
            if (originalImage.empty()) {
                System.out.println("Error: Failed to load image!");
                return;
            }
            
            Mat grayImage = new Mat();
            if (originalImage.channels() == 3) {
                Imgproc.cvtColor(originalImage, grayImage, Imgproc.COLOR_BGR2GRAY);
            } else {
                grayImage = originalImage;
            }
            
            equalizedImage = new Mat();
            Imgproc.equalizeHist(grayImage, equalizedImage);
            
            correctedImage = equalizedImage.clone();
            
            createGUI();
        }
        catch (Exception error) {
            System.out.println("Error: " + error.getMessage());
        }
        finally {
            scanner.close();
        }
    }
    
    private static void createGUI() {
        JFrame frame = new JFrame("Histogram and equalization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        frame.setSize(1400, 900);
        
        JPanel imagePanel = new JPanel(new GridLayout(3, 2, 10, 10));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        originalLabel = new JLabel();
        originalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateImageLabel(originalLabel, originalImage, true);
        
        originalHistLabel = new JLabel();
        originalHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateHistogramLabel(originalHistLabel, originalImage, "Original histogram");
        
        equalizedLabel = new JLabel();
        equalizedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateImageLabel(equalizedLabel, equalizedImage, false);
        
        equalizedHistLabel = new JLabel();
        equalizedHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateHistogramLabel(equalizedHistLabel, equalizedImage, "Equalized histogram");
        
        correctedLabel = new JLabel();
        correctedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateImageLabel(correctedLabel, correctedImage, false);
        
        correctedHistLabel = new JLabel();
        correctedHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateHistogramLabel(correctedHistLabel, correctedImage, "Corrected histogram");
        
        imagePanel.add(createPanelWithTitle(originalLabel, "Original image"));
        imagePanel.add(createPanelWithTitle(originalHistLabel, "Original histogram"));
        imagePanel.add(createPanelWithTitle(equalizedLabel, "After equalization"));
        imagePanel.add(createPanelWithTitle(equalizedHistLabel, "Equalized histogram"));
        imagePanel.add(createPanelWithTitle(correctedLabel, "After correction"));
        imagePanel.add(createPanelWithTitle(correctedHistLabel, "Corrected histogram"));
        
        JPanel controlPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JSlider contrastSlider = new JSlider(0, 2000, 100);
        JLabel contrastValue = new JLabel("1.00");
        setupSlider(contrastSlider, contrastValue, "Contrast (alpha):", controlPanel);
        
        JSlider brightnessSlider = new JSlider(-255, 255, 0);
        JLabel brightnessValue = new JLabel("0");
        setupSlider(brightnessSlider, brightnessValue, "Brightness (beta):", controlPanel);
        
        JSlider gammaSlider = new JSlider(0, 2000, 100);
        JLabel gammaValue = new JLabel("1.00");
        setupSlider(gammaSlider, gammaValue, "Gamma:", controlPanel);
        
        brightnessSlider.addChangeListener(event -> {
            beta = brightnessSlider.getValue();
            brightnessValue.setText(String.valueOf(beta));
            applyCorrections();
        });

        contrastSlider.addChangeListener(event -> {
            alpha = contrastSlider.getValue() / 100.0;
            contrastValue.setText(String.format("%.2f", alpha));
            applyCorrections();
        });

        gammaSlider.addChangeListener(event -> {
            gamma = gammaSlider.getValue() / 100.0;
            gammaValue.setText(String.format("%.2f", gamma));
            applyCorrections();
        });
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(event -> {
            brightnessSlider.setValue(0);
            contrastSlider.setValue(100);
            gammaSlider.setValue(100);
            saveCounter = 1;
            applyCorrections();
        });
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(event -> {
            saveCorrectedImage();
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);
        
        frame.add(imagePanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(buttonPanel, BorderLayout.NORTH);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private static JPanel createPanelWithTitle(JComponent component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }
    
    private static void setupSlider(JSlider slider, JLabel valueLabel, String title, JPanel panel) {
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(new JLabel(title), BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        
        JPanel valuePanel = new JPanel(new BorderLayout());
        valuePanel.add(new JLabel("Value:"), BorderLayout.WEST);
        valuePanel.add(valueLabel, BorderLayout.CENTER);
        
        panel.add(sliderPanel);
        panel.add(valuePanel);
    }
    
    private static void applyCorrections() {
        if (equalizedImage.empty()) return;
        
        Mat tempImage = new Mat();
        equalizedImage.copyTo(tempImage);
        
        if (gamma != 1.0) {
            applyGammaCorrection(tempImage, tempImage, gamma);
        }
        
        tempImage.convertTo(tempImage, -1, alpha, beta);
        
        correctedImage = clampPixelValues(tempImage);
        updateImageLabel(correctedLabel, correctedImage, false);
        updateHistogramLabel(correctedHistLabel, correctedImage, "Corrected histogram");
    }
    
    private static Mat clampPixelValues(Mat image) {
        Mat result = new Mat();
        image.convertTo(result, CvType.CV_8U);
        return result;
    }
    
    private static void applyGammaCorrection(Mat src, Mat dst, double gamma) {
        if (gamma == 1.0) {
            src.copyTo(dst);
            return;
        }
        
        if (gamma <= 0.01) {
            gamma = 0.01;
        }
        
        Mat lut = new Mat(1, 256, CvType.CV_8UC1);
        byte[] lutData = new byte[256];
        
        for (int i = 0; i < 256; i++) {
            lutData[i] = (byte)Math.min(255, Math.max(0, Math.pow(i / 255.0, gamma) * 255));
        }
        
        lut.put(0, 0, lutData);
        Core.LUT(src, lut, dst);
    }
    
    private static void updateImageLabel(JLabel label, Mat image, boolean convertBGRtoRGB) {
        if (image.empty()) return;
        
        Mat displayImage = new Mat();
        
        if (convertBGRtoRGB && image.channels() == 3) {
            Imgproc.cvtColor(image, displayImage, Imgproc.COLOR_BGR2RGB);
        }
        else {
            image.copyTo(displayImage);
        }
        
        BufferedImage bufferedImage = matToBufferedImage(displayImage);
        
        int displayWidth = 300;
        int displayHeight = 250;
        
        Image scaledImage = bufferedImage.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
        label.setIcon(new ImageIcon(scaledImage));
    }
    
    private static void updateHistogramLabel(JLabel label, Mat image, String title) {
        if (image.empty()) return;
        
        Mat histogram = computeHistogram(image);
        BufferedImage histImage = drawHistogram(histogram, 300, 200);
        
        label.setIcon(new ImageIcon(histImage));
        label.setText(title);
        label.setVerticalTextPosition(SwingConstants.TOP);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
    }
    
    private static Mat computeHistogram(Mat image) {
        Mat hist = new Mat();
        MatOfFloat ranges = new MatOfFloat(0, 256);
        MatOfInt histSize = new MatOfInt(256);
        
        Mat grayImage = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            grayImage = image;
        }
        
        Imgproc.calcHist(
            java.util.Arrays.asList(grayImage),
            new MatOfInt(0),
            new Mat(),
            hist,
            histSize,
            ranges
        );
        
        return hist;
    }
    
    private static BufferedImage drawHistogram(Mat histogram, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        Core.MinMaxLocResult result = Core.minMaxLoc(histogram);
        double maxVal = result.maxVal;
        
        g2d.setColor(Color.BLACK);
        g2d.drawLine(30, height - 30, width - 10, height - 30);
        g2d.drawLine(30, height - 30, 30, 10);
        
        g2d.setColor(Color.BLUE);
        int binWidth = (width - 40) / 256;
        
        for (int i = 0; i < 256; i++) {
            double value = histogram.get(i, 0)[0];
            int intensity = (int)((value / maxVal) * (height - 40));
            
            g2d.fillRect(30 + i * binWidth, height - 30 - intensity, 
                        Math.max(1, binWidth - 1), intensity);
        }
        
        g2d.drawString("Brightness", width / 2 - 20, height - 5);
        g2d.drawString("Pixel count", 5, height / 2);
        
        g2d.dispose();
        return image;
    }
    
    private static BufferedImage matToBufferedImage(Mat mat) {
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
    
    private static void saveCorrectedImage() {
        if (originalImagePath == null || originalImagePath.isEmpty() || correctedImage.empty()) {
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
            
            String resultFileName = nameWithoutExtension + "_result_" + saveCounter + extension;
            saveCounter++;
            
            String resultPath;
            if (originalFile.getParent() != null) {
                resultPath = originalFile.getParent() + java.io.File.separator + resultFileName;
            }
            else {
                resultPath = resultFileName;
            }
            
            Mat imageToSave = clampPixelValues(correctedImage);
            
            boolean success = Imgcodecs.imwrite(resultPath, imageToSave);
            if (success) {
                JOptionPane.showMessageDialog(null, "Image saved as: " + resultFileName, "Save", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                JOptionPane.showMessageDialog(null, "Error saving image", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception event) {
            JOptionPane.showMessageDialog(null, "Error saving image", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
