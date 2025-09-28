import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.Scanner;

public class Main implements ComponentListener {
    private Mat originalImage;
    private Mat equalizedImage;
    private Mat correctedImage;
    private JLabel originalLabel;
    private JLabel equalizedLabel;
    private JLabel correctedLabel;
    private JLabel originalHistLabel;
    private JLabel originalCumulativeHistLabel;
    private JLabel equalizedHistLabel;
    private JLabel equalizedCumulativeHistLabel;
    private JLabel correctedHistLabel;
    private JLabel correctedCumulativeHistLabel;
    
    private double alpha = 1.0;
    private double beta = 0.0;
    private double gamma = 1.0;

    private String originalImagePath;
    private int saveCounter = 1;
    
    private JSlider contrastSlider;
    private JSlider brightnessSlider;
    private JSlider gammaSlider;
    
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
            
            Mat grayImage = new Mat();
            if (originalImage.channels() == 3) {
                Imgproc.cvtColor(originalImage, grayImage, Imgproc.COLOR_BGR2GRAY);
            }
            else {
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
    
    private void createGUI() {
        JFrame frame = new JFrame("Histogram and equalization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int minWidth = (int)(screenSize.width * 0.3);
        int minHeight = (int)(screenSize.height * 0.3);
        frame.setMinimumSize(new Dimension(minWidth, minHeight));
        
        frame.setSize(1400, 900);
        
        JPanel imagePanel = new JPanel(new GridLayout(3, 3, 5, 5));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        originalLabel = new JLabel();
        originalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        originalHistLabel = new JLabel();
        originalHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        originalCumulativeHistLabel = new JLabel();
        originalCumulativeHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        equalizedLabel = new JLabel();
        equalizedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        equalizedHistLabel = new JLabel();
        equalizedHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        equalizedCumulativeHistLabel = new JLabel();
        equalizedCumulativeHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        correctedLabel = new JLabel();
        correctedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        correctedHistLabel = new JLabel();
        correctedHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        correctedCumulativeHistLabel = new JLabel();
        correctedCumulativeHistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        imagePanel.add(createPanelWithTitle(originalLabel, "Original image"));
        imagePanel.add(createPanelWithTitle(originalHistLabel, "Original histogram"));
        imagePanel.add(createPanelWithTitle(originalCumulativeHistLabel, "Original cumulative histogram"));
        imagePanel.add(createPanelWithTitle(equalizedLabel, "Equalized image"));
        imagePanel.add(createPanelWithTitle(equalizedHistLabel, "Equalized histogram"));
        imagePanel.add(createPanelWithTitle(equalizedCumulativeHistLabel, "Equalized cumulative histogram"));
        imagePanel.add(createPanelWithTitle(correctedLabel, "Corrected image"));
        imagePanel.add(createPanelWithTitle(correctedHistLabel, "Corrected histogram"));
        imagePanel.add(createPanelWithTitle(correctedCumulativeHistLabel, "Corrected cumulative histogram"));
        
        updateAllDisplays();
        
        JPanel controlPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        contrastSlider = new JSlider(0, 2000, 100);
        JLabel contrastValue = new JLabel("1.00");
        setupSlider(contrastSlider, contrastValue, "Contrast (alpha):", controlPanel);
        
        brightnessSlider = new JSlider(-255, 255, 0);
        JLabel brightnessValue = new JLabel("0");
        setupSlider(brightnessSlider, brightnessValue, "Brightness (beta):", controlPanel);
        
        gammaSlider = new JSlider(0, 2000, 100);
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
        resetButton.addActionListener(event -> resetParameters());
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(event -> saveCorrectedImage());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);
        
        frame.add(imagePanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(buttonPanel, BorderLayout.NORTH);
        
        frame.addComponentListener(this);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private void resetParameters() {
        alpha = 1.0;
        beta = 0.0;
        gamma = 1.0;
        
        contrastSlider.setValue(100);
        brightnessSlider.setValue(0);
        gammaSlider.setValue(100);
        
        applyCorrections();
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
        updateHistogramLabels(originalHistLabel, originalCumulativeHistLabel, originalImage, Color.BLUE);
        updateImageLabel(equalizedLabel, equalizedImage, false);
        updateHistogramLabels(equalizedHistLabel, equalizedCumulativeHistLabel, equalizedImage, Color.RED);
        updateImageLabel(correctedLabel, correctedImage, false);
        updateHistogramLabels(correctedHistLabel, correctedCumulativeHistLabel, correctedImage, Color.GREEN);
    }
    
    private void updateHistogramLabels(JLabel histLabel, JLabel cumulativeHistLabel, Mat image, Color color) {
        if (image.empty()) {
            return;
        }
        
        Mat histogram = computeHistogram(image);
        Mat cumulativeHistogram = computeCumulativeHistogram(histogram);
        
        Dimension panelSize = ((JPanel)histLabel.getParent().getParent()).getSize();
        
        int availableWidth = (panelSize.width - 20) / 4;
        int availableHeight = (panelSize.height - 40) / 4;
        
        double histRatio = 1.5;
        
        int width, height;
        
        if (histRatio > 1.0) {
            width = availableWidth;
            height = (int)(availableWidth / histRatio);
        }
        else {
            height = availableHeight;
            width = (int)(availableHeight * histRatio);
        }
        
        width = Math.max(120, Math.min(width, availableWidth));
        height = Math.max(80, Math.min(height, availableHeight));
        
        BufferedImage histImage = drawHistogram(histogram, width, height, color, false);
        BufferedImage cumulativeHistImage = drawHistogram(cumulativeHistogram, width, height, color, true);
        
        histLabel.setIcon(new ImageIcon(histImage));
        cumulativeHistLabel.setIcon(new ImageIcon(cumulativeHistImage));
    }
    
    private void applyCorrections() {
        if (equalizedImage.empty()) {
            return;
        }
        
        Mat tempImage = new Mat();
        equalizedImage.copyTo(tempImage);
        
        if (gamma != 1.0) {
            applyGammaCorrection(tempImage, tempImage, gamma);
        }
        
        tempImage.convertTo(tempImage, -1, alpha, beta);
        
        correctedImage = clampPixelValues(tempImage);
        updateImageLabel(correctedLabel, correctedImage, false);
        updateHistogramLabels(correctedHistLabel, correctedCumulativeHistLabel, correctedImage, Color.GREEN);
    }
    
    private Mat clampPixelValues(Mat image) {
        Mat result = new Mat();
        image.convertTo(result, CvType.CV_8U);
        return result;
    }
    
    private void applyGammaCorrection(Mat src, Mat dst, double gamma) {
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
        
        Dimension panelSize = ((JPanel)label.getParent().getParent()).getSize();
        int availableWidth = (panelSize.width - 20) / 4;
        int availableHeight = (panelSize.height - 40) / 4;
        
        double imageRatio = (double)image.cols() / image.rows();
        double panelRatio = (double)availableWidth / availableHeight;
        
        int displayWidth, displayHeight;
        
        if (imageRatio > panelRatio) {
            displayWidth = availableWidth;
            displayHeight = (int)(availableWidth / imageRatio);
        }
        else {
            displayHeight = availableHeight;
            displayWidth = (int)(availableHeight * imageRatio);
        }
        
        displayWidth = Math.max(100, Math.min(displayWidth, availableWidth));
        displayHeight = Math.max(100, Math.min(displayHeight, availableHeight));
        
        Image scaledImage = bufferedImage.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
        label.setIcon(new ImageIcon(scaledImage));
    }
    
    private Mat computeHistogram(Mat image) {
        Mat hist = new Mat();
        MatOfFloat ranges = new MatOfFloat(0, 256);
        MatOfInt histSize = new MatOfInt(256);
        
        Mat grayImage = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        }
        else {
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
    
    private Mat computeCumulativeHistogram(Mat histogram) {
        Mat cumulative = new Mat(histogram.size(), histogram.type());
        double sum = 0;
        
        for (int i = 0; i < histogram.rows(); i++) {
            sum += histogram.get(i, 0)[0];
            cumulative.put(i, 0, sum);
        }
        
        return cumulative;
    }
    
    private BufferedImage drawHistogram(Mat histogram, int width, int height, Color color, boolean isCumulative) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        Core.MinMaxLocResult result = Core.minMaxLoc(histogram);
        double maxVal = result.maxVal;
        
        int padding = 2;
        g2d.setColor(Color.BLACK);
        g2d.drawLine(padding, height - padding, width - padding, height - padding);
        g2d.drawLine(padding, height - padding, padding, padding);
        
        g2d.setColor(color);
        
        int availableWidth = width - padding * 2;
        int totalBins = 256;
        
        float binWidth = (float)availableWidth / totalBins;
        
        for (int i = 0; i < totalBins; i++) {
            double value = histogram.get(i, 0)[0];
            int intensity = (int)((value / maxVal) * (height - padding * 2));
            
            int x = padding + (int)(i * binWidth);
            int binRenderWidth = (int)Math.ceil(binWidth);
            
            if (isCumulative) {
                if (i > 0) {
                    double prevValue = histogram.get(i - 1, 0)[0];
                    int prevIntensity = (int)((prevValue / maxVal) * (height - padding * 2));
                    int prevX = padding + (int)((i - 1) * binWidth);
                    g2d.drawLine(prevX, height - padding - prevIntensity, x, height - padding - intensity);
                }
            }
            else {
                g2d.fillRect(x, height - padding - intensity, binRenderWidth, intensity);
            }
        }
        
        g2d.dispose();
        return image;
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
    
    private void saveCorrectedImage() {
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

    @Override
    public void componentResized(ComponentEvent event) {
        updateAllDisplays();
    }

    @Override
    public void componentMoved(ComponentEvent event) {}

    @Override
    public void componentShown(ComponentEvent event) {}

    @Override
    public void componentHidden(ComponentEvent event) {}
}
