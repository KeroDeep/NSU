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
    
    private int noiseLevel;
    private int requiredImages;
    private double currentPSNR;
    private String imagePath;
    private String originalFileName;
    
    private Random random = new Random();
    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new Main();
    }
    
    public Main() {
        getUserInput();
        loadImage();
        findRequiredImages();
        createGUI();
    }
    
    private void getUserInput() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter the path to the image: ");
        imagePath = scanner.nextLine();
        
        System.out.print("Enter the noise level: ");
        noiseLevel = scanner.nextInt();
        
        scanner.close();
        
        File file = new File(imagePath);
        originalFileName = file.getName();
        int dotIndex = originalFileName.lastIndexOf('.');

        if (dotIndex > 0) {
            originalFileName = originalFileName.substring(0, dotIndex);
        }
    }
    
    private void loadImage() {
        originalImage = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR);
        
        if (originalImage.empty()) {
            createTestImage();
        }
        
        grayscaleImage = new Mat();
        Imgproc.cvtColor(originalImage, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
        
        restoredImage = new Mat(grayscaleImage.size(), grayscaleImage.type(), new Scalar(0));
        noisyImages = new ArrayList<>();
        noisyLabels = new ArrayList<>();
    }
    
    private void createTestImage() {
        originalImage = new Mat(400, 400, CvType.CV_8UC3, new Scalar(255, 255, 255));
        Imgproc.rectangle(originalImage, new org.opencv.core.Point(50, 50), new org.opencv.core.Point(150, 150), new Scalar(0, 0, 255), -1);
        Imgproc.circle(originalImage, new org.opencv.core.Point(300, 200), 60, new Scalar(255, 0, 0), -1);
        Imgproc.line(originalImage, new org.opencv.core.Point(100, 300), new org.opencv.core.Point(350, 350), new Scalar(0, 255, 0), 3);
    }
    
    private void findRequiredImages() {
        requiredImages = 1;
        double targetPSNR = 30.0;
        
        while (requiredImages <= 100) {
            generateNoisyImages(requiredImages);
            restoreImage();
            
            if (currentPSNR >= targetPSNR || requiredImages >= 100) {
                break;
            }
            
            requiredImages++;
        }
        
        System.out.println("Required noisy images: " + requiredImages);
        System.out.println("Achieved PSNR: " + String.format("%.2f", currentPSNR) + " dB");
    }
    
    private void generateNoisyImages(int count) {
        noisyImages.clear();

        for (int i = 0; i < count; i++) {
            Mat noisyImage = addUniformNoise(grayscaleImage);
            noisyImages.add(noisyImage);
        }
    }
    
    private Mat addUniformNoise(Mat image) {
        Mat noisyImage = new Mat();
        image.copyTo(noisyImage);
        
        int halfT = noiseLevel / 2;
        
        for (int row = 0; row < image.rows(); row++) {
            for (int col = 0; col < image.cols(); col++) {
                double[] pixel = image.get(row, col);
                double originalValue = pixel[0];
                
                double noise = random.nextDouble() * noiseLevel - halfT;
                
                double newValue = originalValue + noise;
                
                newValue = Math.max(0, Math.min(255, newValue));
                
                noisyImage.put(row, col, newValue);
            }
        }
        
        return noisyImage;
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
        JFrame frame = new JFrame("Image denoising");
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
        
        int displayCount = Math.min(requiredImages, 3);
        for (int i = 0; i < 3; i++) {
            if (i < displayCount) {
                JLabel noisyLabel = new JLabel("Noisy " + (i + 1), SwingConstants.CENTER);
                noisyLabels.add(noisyLabel);
                mainPanel.add(createImagePanel(noisyLabel, noisyImages.get(i), "Noisy " + (i + 1)));
            }
            else {
                mainPanel.add(createEmptyPanel());
            }
        }
        
        JButton saveButton = new JButton("Save restored image");
        saveButton.addActionListener(e -> saveResults());
        
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
        if (restoredImage.empty()) return;
        
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
