import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static List<BufferedImage> originalColorImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();

    private static FileManager currentFileManager = null;
    
    private static final int PATTERN_WIDTH = 100;
    private static final double MAX_DEPTH = 0.15;
    
    private static Mat currentStereogram = null;
    private static BufferedImage currentStereogramImage = null;
    private static String currentImagePath = null;
    private static int originalWidth = 0;
    private static int originalHeight = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Autostereogram generator");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(1200, 800);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                
                imagePanel = new JPanel(new BorderLayout());
                imagePanel.setBackground(Color.WHITE);
                imagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                
                mainFrame.add(controlPanel, BorderLayout.NORTH);
                mainFrame.add(imagePanel, BorderLayout.CENTER);
                
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }
    
    private static JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton openBtn = new JButton("Open file manager");
        JButton createStereogramBtn = new JButton("Create autostereogram");
        JButton saveStereogramBtn = new JButton("Save autostereogram");
        
        openBtn.addActionListener(event -> openFileManager());
        createStereogramBtn.addActionListener(event -> createStereogramFromLoadedImages());
        saveStereogramBtn.addActionListener(event -> saveStereogram());
        
        panel.add(openBtn);
        panel.add(createStereogramBtn);
        panel.add(saveStereogramBtn);
        
        return panel;
    }
    
    private static void createStereogramFromLoadedImages() {
        if (originalColorImages.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Please load an image first.");

            return;
        }
        
        try {
            BufferedImage inputImage = originalColorImages.get(0);
            originalWidth = inputImage.getWidth();
            originalHeight = inputImage.getHeight();
            
            Mat inputMat = bufferedImageToMat(inputImage);

            if (inputMat == null) {
                JOptionPane.showMessageDialog(mainFrame, "Error: Could not convert image to OpenCV format");

                return;
            }
            
            Mat depthMap = createDepthMapFromImage(inputMat);
            
            currentStereogram = generateStereogram(depthMap);
            currentStereogramImage = matToBufferedImage(currentStereogram);
            
            inputMat.release();
            depthMap.release();
            
            refreshDisplay();
            
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error creating stereogram: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
    
    private static Mat createDepthMapFromImage(Mat inputImage) {
        Mat gray = new Mat();

        if (inputImage.channels() == 3) {
            Imgproc.cvtColor(inputImage, gray, Imgproc.COLOR_BGR2GRAY);
        }
        else {
            gray = inputImage.clone();
        }
        
        Mat binary = new Mat();
        Imgproc.adaptiveThreshold(gray, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        
        Mat inverted = new Mat();
        Core.bitwise_not(binary, inverted);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(inverted, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        Mat depthMap = Mat.zeros(originalHeight, originalWidth, CvType.CV_8UC1);
        
        if (!contours.isEmpty()) {
            MatOfPoint largestContour = contours.stream().max((c1, c2) -> Double.compare(Imgproc.contourArea(c1), Imgproc.contourArea(c2))).orElse(contours.get(0));
            
            Mat mask = Mat.zeros(inverted.size(), CvType.CV_8UC1);
            Imgproc.drawContours(mask, java.util.Arrays.asList(largestContour), -1, new Scalar(255), -1);
            
            Mat distTransform = new Mat();
            Imgproc.distanceTransform(mask, distTransform, Imgproc.DIST_L2, Imgproc.DIST_MASK_PRECISE);
            
            Core.normalize(distTransform, distTransform, 0, 255, Core.NORM_MINMAX);
            distTransform.convertTo(distTransform, CvType.CV_8UC1);
            
            Imgproc.resize(distTransform, depthMap, new Size(originalWidth, originalHeight));
            
            enhanceDepthMap(depthMap);
        }
        else {
            for (int y = 0; y < originalHeight; y++) {
                for (int x = 0; x < originalWidth; x++) {
                    depthMap.put(y, x, 128);
                }
            }
        }
        
        gray.release();
        binary.release();
        inverted.release();
        hierarchy.release();
        
        return depthMap;
    }
    
    private static void enhanceDepthMap(Mat depthMap) {
        Imgproc.GaussianBlur(depthMap, depthMap, new Size(15, 15), 0);
        Core.normalize(depthMap, depthMap, 50, 255, Core.NORM_MINMAX);
    }
    
    private static Mat generateStereogram(Mat depthMap) {
        Mat stereogram = new Mat(originalHeight, originalWidth, CvType.CV_8UC3);
        
        Mat pattern = generateColorPattern(PATTERN_WIDTH, originalHeight);
        
        Mat normalizedDepth = new Mat();
        depthMap.convertTo(normalizedDepth, CvType.CV_64F, 1.0/255.0);
        
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < PATTERN_WIDTH; x++) {
                double[] pixel = pattern.get(y, x);
                stereogram.put(y, x, pixel);
            }
        }
        
        for (int y = 0; y < originalHeight; y++) {
            for (int x = PATTERN_WIDTH; x < originalWidth; x++) {
                double depth = normalizedDepth.get(y, x)[0];
                int shift = (int) (PATTERN_WIDTH * MAX_DEPTH * depth);
                int sourceX = Math.max(0, x - PATTERN_WIDTH + shift);
                
                double[] pixel = stereogram.get(y, sourceX);
                stereogram.put(y, x, pixel);
            }
        }
        
        pattern.release();
        normalizedDepth.release();
        
        return stereogram;
    }
    
    private static Mat generateColorPattern(int width, int height) {
        Mat pattern = new Mat(height, width, CvType.CV_8UC3);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double[] color = {
                    Math.random() * 256,
                    Math.random() * 256,
                    Math.random() * 256
                };

                pattern.put(y, x, color);
            }
        }
        
        Mat kernel = Imgproc.getGaussianKernel(7, 1.5);
        Imgproc.filter2D(pattern, pattern, -1, kernel);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double[] pixel = pattern.get(y, x);

                for (int c = 0; c < 3; c++) {
                    pixel[c] = Math.min(255, pixel[c] * 1.2);
                }

                pattern.put(y, x, pixel);
            }
        }
        
        kernel.release();

        return pattern;
    }
    
    private static void saveStereogram() {
        if (currentStereogram == null || currentImagePath == null) {
            JOptionPane.showMessageDialog(mainFrame, "No stereogram created yet.");

            return;
        }
        
        try {
            File originalFile = new File(currentImagePath);
            String originalName = originalFile.getName();
            String nameWithoutExtension = originalName.substring(0, originalName.lastIndexOf('.'));
            String extension = originalName.substring(originalName.lastIndexOf('.'));
            
            String outputFileName = nameWithoutExtension + "_autostereogram" + extension;
            String outputPath = outputFileName;
            
            boolean success = Imgcodecs.imwrite(outputPath, currentStereogram);

            if (success) {
                JOptionPane.showMessageDialog(mainFrame, "Stereogram saved as: " + outputFileName);
            }
            else {
                JOptionPane.showMessageDialog(mainFrame, "Failed to save stereogram.");
            }
            
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving stereogram: " + exception.getMessage());
            exception.printStackTrace();
        }
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
        originalColorImages.clear();
        imagePaths.clear();
        currentStereogram = null;
        currentStereogramImage = null;
        currentImagePath = null;
        originalWidth = 0;
        originalHeight = 0;
        
        for (String path : paths) {
            try {
                BufferedImage colorImage = ImageIO.read(new File(path));

                if (colorImage != null) {
                    originalColorImages.add(colorImage);
                    imagePaths.add(path);
                    currentImagePath = path;
                    originalWidth = colorImage.getWidth();
                    originalHeight = colorImage.getHeight();
                }
            }
            catch (Exception event) {
                System.err.println("Error loading image: " + path);
                event.printStackTrace();
            }
        }
    }
    
    private static void refreshDisplay() {
        imagePanel.removeAll();
        
        if (originalColorImages.isEmpty()) {
            imagePanel.revalidate();
            imagePanel.repaint();

            return;
        }
        
        if (currentStereogramImage != null) {
            displayStereogramResult();
        }
        else {
            displayLoadedImage();
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static void displayStereogramResult() {
        if (currentStereogramImage == null) {
            return;
        }
        
        int maxWidth = mainFrame.getWidth() - 100;
        int maxHeight = mainFrame.getHeight() - 150;
        
        int imageWidth = currentStereogramImage.getWidth();
        int imageHeight = currentStereogramImage.getHeight();
        
        double widthRatio = (double) maxWidth / imageWidth;
        double heightRatio = (double) maxHeight / imageHeight;
        double scale = Math.min(widthRatio, heightRatio);
        
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);
        
        Image scaledImage = currentStereogramImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImage);
        JLabel imageLabel = new JLabel(icon);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setBackground(Color.WHITE);
        imageContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        imageContainer.add(imageLabel, BorderLayout.CENTER);
        
        imagePanel.add(imageContainer, BorderLayout.CENTER);
    }
    
    private static void displayLoadedImage() {
        BufferedImage image = originalColorImages.get(0);
        
        int maxWidth = mainFrame.getWidth() - 100;
        int maxHeight = mainFrame.getHeight() - 150;
        
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        
        double widthRatio = (double) maxWidth / imageWidth;
        double heightRatio = (double) maxHeight / imageHeight;
        double scale = Math.min(widthRatio, heightRatio);
        
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);
        
        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImage);
        JLabel imageLabel = new JLabel(icon);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setBackground(Color.WHITE);
        imageContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        imageContainer.add(imageLabel, BorderLayout.CENTER);
        
        imagePanel.add(imageContainer, BorderLayout.CENTER);
    }
    
    private static Mat bufferedImageToMat(BufferedImage image) {
        try {
            BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = convertedImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(convertedImage, "png", byteArrayOutputStream);
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
    
    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".png", mat, mob);
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
}
