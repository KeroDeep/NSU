import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private Mat originalImage;
    private Mat grayscaleImage;
    private List<Mat> noisyImages;
    private Mat restoredImage;
    
    private JLabel originalColorLabel;
    private JLabel originalGrayLabel;
    private JLabel restoredLabel;
    private List<JLabel> noisyLabels;
    
    private int requiredImages;
    private double currentPSNR;
    private String imagePath;
    private String originalFileName;
    
    private Random random = new Random();
    private static final int maxNoisyImages = 100;
    private static final int maxStripesCount = 3;
    private static final double TargetPSNR = 30.0;
    private static final double mimStripeWidthPercent = 0.02;
    private static final double maxStripeWidthPercent = 0.08;
    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new Main();
    }
    
    public Main() {
        loadImageWithInput();
        findRequiredImages();
        createGUI();
    }
    
    private void loadImageWithInput() {
        Scanner scanner = new Scanner(System.in);
        boolean loaded = false;
        
        while (!loaded) {
            System.out.print("Enter the path to the image: ");
            imagePath = scanner.nextLine().trim();
            
            originalImage = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR);
            
            if (originalImage.empty()) {
                System.out.println("Invalid path or image not found!");
                System.out.println("Please try again.");
            }
            else {
                loaded = true;
            }
        }
        
        scanner.close();
        
        File file = new File(imagePath);
        originalFileName = file.getName();
        int dotIndex = originalFileName.lastIndexOf('.');

        if (dotIndex > 0) {
            originalFileName = originalFileName.substring(0, dotIndex);
        }
        
        grayscaleImage = new Mat();
        Imgproc.cvtColor(originalImage, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
        
        restoredImage = new Mat(grayscaleImage.size(), grayscaleImage.type(), new Scalar(0));
        noisyImages = new ArrayList<>();
        noisyLabels = new ArrayList<>();
    }
    
    private void findRequiredImages() {
        requiredImages = 3;
        generateInitialNoisyImages();
        restoreImage();
        
        while (currentPSNR < TargetPSNR && requiredImages < maxNoisyImages) {
            Mat newNoisyImage = addStripedNoise(grayscaleImage);
            noisyImages.add(newNoisyImage);
            requiredImages++;
            restoreImage();
        }
        
        System.out.println("Required noisy images: " + requiredImages);
        System.out.println("Peak Signal-to-Noise Ratio (PSNR)");
        System.out.println("Achieved PSNR: " + String.format("%.2f", currentPSNR) + " dB");
    }
    
    private void generateInitialNoisyImages() {
        for (int i = 0; i < 3; i++) {
            Mat noisyImage = addStripedNoise(grayscaleImage);
            noisyImages.add(noisyImage);
        }
    }
    
    private Mat addStripedNoise(Mat image) {
        Mat noisyImage = new Mat();
        image.copyTo(noisyImage);
        
        int stripesCount = random.nextInt(maxStripesCount - 3 + 1) + 3;
        
        for (int i = 0; i < stripesCount; i++) {
            boolean isVertical = random.nextBoolean();
            
            if (isVertical) {
                int minWidth = Math.max(1, (int) (image.cols() * mimStripeWidthPercent));
                int maxWidth = Math.max(1, (int) (image.cols() * maxStripeWidthPercent));
                int stripeWidth = random.nextInt(maxWidth - minWidth + 1) + minWidth;
                int startCol = random.nextInt(Math.max(1, image.cols() - stripeWidth));
                addVerticalStripeNoise(noisyImage, startCol, stripeWidth);
            }
            else {
                int minWidth = Math.max(1, (int) (image.rows() * mimStripeWidthPercent));
                int maxWidth = Math.max(1, (int) (image.rows() * maxStripeWidthPercent));
                int stripeWidth = random.nextInt(maxWidth - minWidth + 1) + minWidth;
                int startRow = random.nextInt(Math.max(1, image.rows() - stripeWidth));
                addHorizontalStripeNoise(noisyImage, startRow, stripeWidth);
            }
        }
        
        return noisyImage;
    }
    
    private void addVerticalStripeNoise(Mat image, int startCol, int width) {
        for (int col = startCol; col < startCol + width && col < image.cols(); col++) {
            for (int row = 0; row < image.rows(); row++) {
                double randomValue = random.nextInt(256);
                image.put(row, col, randomValue);
            }
        }
    }
    
    private void addHorizontalStripeNoise(Mat image, int startRow, int width) {
        for (int row = startRow; row < startRow + width && row < image.rows(); row++) {
            for (int col = 0; col < image.cols(); col++) {
                double randomValue = random.nextInt(256);
                image.put(row, col, randomValue);
            }
        }
    }
    
    private void restoreImage() {
        Mat accumulated = new Mat(grayscaleImage.size(), CvType.CV_32F, new Scalar(0));
        
        for (Mat noisyImage : noisyImages) {
            Mat floatImage = new Mat();
            noisyImage.convertTo(floatImage, CvType.CV_32F);
            Core.add(accumulated, floatImage, accumulated);
        }
        
        Core.divide(accumulated, new Scalar(noisyImages.size()), accumulated);
        accumulated.convertTo(restoredImage, CvType.CV_8U);
        
        currentPSNR = calculatePSNR(grayscaleImage, restoredImage);
    }
    
    private double calculatePSNR(Mat original, Mat restored) {
        Mat diff = new Mat();
        Core.absdiff(original, restored, diff);
        
        Mat squaredDiff = new Mat();
        Core.pow(diff, 2, squaredDiff);
        
        Scalar mseScalar = Core.mean(squaredDiff);
        double mse = mseScalar.val[0];
        
        if (mse == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }
    
    private void createGUI() {
        JFrame frame = new JFrame("Image denoising with striped noise");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        originalColorLabel = new JLabel("Color original image", SwingConstants.CENTER);
        originalGrayLabel = new JLabel("Black and white original image", SwingConstants.CENTER);
        restoredLabel = new JLabel("Restored image", SwingConstants.CENTER);
        
        mainPanel.add(createImagePanel(originalColorLabel, originalImage, "Color original image"));
        mainPanel.add(createImagePanel(originalGrayLabel, grayscaleImage, "Black and white original image"));
        mainPanel.add(createImagePanel(restoredLabel, restoredImage, "Restored image"));
        
        for (int i = 0; i < 3; i++) {
            if (i < noisyImages.size()) {
                JLabel noisyLabel = new JLabel("Noisy " + (i + 1), SwingConstants.CENTER);
                noisyLabels.add(noisyLabel);
                mainPanel.add(createImagePanel(noisyLabel, noisyImages.get(i), "Noisy " + (i + 1)));
            }
            else {
                mainPanel.add(createEmptyPanel());
            }
        }
        
        JButton saveButton = new JButton("Save restored image");
        saveButton.addActionListener(event -> saveResults());
        
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(saveButton, BorderLayout.SOUTH);
        
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private JPanel createImagePanel(JLabel label, Mat image, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        if (image != null && !image.empty()) {
            BufferedImage bufferedImage = matToBufferedImage(image);
            Image scaledImage = bufferedImage.getScaledInstance(250, 250, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaledImage));
            label.setText("");
        }
        
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }
    
    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat.empty()) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        }
        
        Mat tempMat = new Mat();
        if (mat.depth() != CvType.CV_8U) {
            mat.convertTo(tempMat, CvType.CV_8U);
        }
        else {
            tempMat = mat;
        }
        
        if (tempMat.channels() == 3) {
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(tempMat, rgbMat, Imgproc.COLOR_BGR2RGB);
            tempMat = rgbMat;
        }
        
        int type = (tempMat.channels() == 1) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
        
        byte[] buffer = new byte[tempMat.channels() * tempMat.cols() * tempMat.rows()];
        tempMat.get(0, 0, buffer);
        
        BufferedImage image = new BufferedImage(tempMat.cols(), tempMat.rows(), type);
        image.getRaster().setDataElements(0, 0, tempMat.cols(), tempMat.rows(), buffer);
        
        return image;
    }
    
    private JPanel createEmptyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("No image"));
        panel.add(new JLabel("No image", SwingConstants.CENTER), BorderLayout.CENTER);

        return panel;
    }
    
    private void saveResults() {
        if (restoredImage.empty()) {
            return;
        }
        
        File originalFile = new File(imagePath);
        String parentPath = originalFile.getParent();
        String extension = "";
        String originalName = originalFile.getName();
        
        int dotIndex = originalName.lastIndexOf('.');

        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
            originalName = originalName.substring(0, dotIndex);
        }
        
        String restoredPath;
        if (parentPath != null) {
            restoredPath = parentPath + File.separator + originalName + "_restored" + extension;
        }
        else {
            restoredPath = originalName + "_restored" + extension;
        }
        
        boolean success = Imgcodecs.imwrite(restoredPath, restoredImage);
        
        if (success) {
            JOptionPane.showMessageDialog(null, "Restored image saved as: " + originalName + "_restored" + extension, "Save", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
