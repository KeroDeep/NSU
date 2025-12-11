import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.ml.SVM;

class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static JLabel infoLabel;
    private static JButton loadImageBtn, clearBtn;
    private static SVM characterSVM;
    
    private static List<BufferedImage> originalImages = new ArrayList<>();
    private static List<BufferedImage> plateROIs = new ArrayList<>();
    private static List<BufferedImage> binaryChars = new ArrayList<>();
    private static List<BufferedImage> recognizedChars = new ArrayList<>();
    
    private static FileManager currentFileManager = null;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("License plate recognition");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                imagePanel = new JPanel(new GridLayout(0, 4, 10, 10));
                imagePanel.setBackground(Color.WHITE);
                imagePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                
                JScrollPane scrollPane = new JScrollPane(imagePanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                infoLabel = new JLabel(" ", JLabel.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.setBackground(Color.WHITE);
                topPanel.add(controlPanel, BorderLayout.NORTH);
                topPanel.add(infoLabel, BorderLayout.CENTER);
                
                mainFrame.add(topPanel, BorderLayout.NORTH);
                mainFrame.add(scrollPane, BorderLayout.CENTER);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
                initializeSVM();
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }
    
    private static JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        loadImageBtn = new JButton("Open file manager");
        clearBtn = new JButton("Clear all");
        
        loadImageBtn.addActionListener(exception -> openFileManager());
        clearBtn.addActionListener(exception -> clearAll());
        
        panel.add(loadImageBtn);
        panel.add(clearBtn);
        
        return panel;
    }
    
    private static void openFileManager() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(imagePaths -> {
            currentFileManager.close();
            currentFileManager = null;

            if (imagePaths != null && imagePaths.length > 0) {
                int loadedCount = 0;
                
                for (String imagePath : imagePaths) {
                    try {
                        File file = new File(imagePath);
                        BufferedImage image = ImageIO.read(file);

                        if (image != null) {
                            originalImages.add(image);
                            loadedCount++;
                            processSingleImage(image);
                        }
                    }
                    catch (Exception exception) {
                        System.err.println("Error loading image: " + imagePath);
                    }
                }
                
                if (loadedCount > 0) {
                    displayResultsTable();
                }
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void processSingleImage(BufferedImage original) {
        try {
            Mat originalMat = bufferedImageToMat(original);
            
            List<Rect> plateRects = detectLicensePlates(originalMat);
            
            if (!plateRects.isEmpty()) {
                Rect plateRect = plateRects.get(0);
                Mat plateMat = extractLicensePlate(originalMat, plateRect);
                List<Mat> characters = segmentCharacters(plateMat);
                
                BufferedImage plateROI = matToBufferedImage(plateMat);
                BufferedImage binaryImage = createBinaryCharactersImage(characters);
                BufferedImage recognizedImage = createRecognizedCharactersImage(characters);
                
                plateROIs.add(plateROI);
                binaryChars.add(binaryImage);
                recognizedChars.add(recognizedImage);
                
                originalMat.release();
                plateMat.release();
                
                for (Mat character : characters) {
                    character.release();
                }
            }
            else {
                plateROIs.add(createPlaceholderImage(400, 100, "Plate not found"));
                binaryChars.add(createPlaceholderImage(400, 100, "No characters"));
                recognizedChars.add(createPlaceholderImage(400, 100, "No characters"));
            }
        }
        catch (Exception exception) {
            System.err.println("Error processing image: " + exception.getMessage());
            plateROIs.add(createPlaceholderImage(400, 100, "Processing error"));
            binaryChars.add(createPlaceholderImage(400, 100, "Error"));
            recognizedChars.add(createPlaceholderImage(400, 100, "Error"));
        }
    }
    
    private static void initializeSVM() {
        try {
            characterSVM = SVM.create();
            characterSVM.setType(SVM.C_SVC);
            characterSVM.setKernel(SVM.LINEAR);
            characterSVM.setC(1);
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error initializing SVM", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static BufferedImage createPlaceholderImage(int width, int height, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, height / 2);
        g2d.dispose();

        return image;
    }
    
    private static List<Rect> detectLicensePlates(Mat image) {
        List<Rect> plates = new ArrayList<>();
        
        Mat gray = new Mat();

        if (image.channels() > 1) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        }
        else {
            gray = image.clone();
        }
        
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
        
        Mat edges = new Mat();
        Imgproc.Canny(blurred, edges, 50, 150);
        
        Mat dilated = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(edges, dilated, kernel, new org.opencv.core.Point(-1, -1), 2);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) rect.width / rect.height;
            double area = rect.area();
            double imageArea = image.rows() * image.cols();
            
            if (area > imageArea * 0.005 && area < imageArea * 0.3 &&
                aspectRatio > 1.8 && aspectRatio < 4.5) {
                plates.add(rect);
            }
        }
        
        plates.sort((r1, r2) -> Double.compare(r2.area(), r1.area()));
        
        gray.release();
        blurred.release();
        edges.release();
        dilated.release();
        hierarchy.release();
        kernel.release();
        
        return plates;
    }
    
    private static Mat extractLicensePlate(Mat original, Rect plateRect) {
        int padding = 5;
        int x = Math.max(0, plateRect.x - padding);
        int y = Math.max(0, plateRect.y - padding);
        int width = Math.min(original.cols() - x, plateRect.width + 2 * padding);
        int height = Math.min(original.rows() - y, plateRect.height + 2 * padding);
        
        Rect expandedRect = new Rect(x, y, width, height);

        return new Mat(original, expandedRect);
    }
    
    private static List<Mat> segmentCharacters(Mat plateImage) {
        List<Mat> characters = new ArrayList<>();
        
        Mat plateGray = new Mat();

        if (plateImage.channels() > 1) {
            Imgproc.cvtColor(plateImage, plateGray, Imgproc.COLOR_BGR2GRAY);
        }
        else {
            plateGray = plateImage.clone();
        }
        
        Mat plateBinary = new Mat();
        Imgproc.threshold(plateGray, plateBinary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        
        if (isWhiteOnBlack(plateBinary)) {
            Core.bitwise_not(plateBinary, plateBinary);
        }
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(plateBinary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        List<Rect> charRects = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            
            if (rect.width > 8 && rect.height > 20 && 
                rect.width < plateBinary.cols() / 2 && 
                rect.height > plateBinary.rows() / 3) {
                charRects.add(rect);
            }
        }
        
        charRects.sort((r1, r2) -> Integer.compare(r1.x, r2.x));
        
        for (Rect rect : charRects) {
            Mat charMat = new Mat(plateBinary, rect);
            Mat cleanedChar = cleanCharacter(charMat);
            characters.add(cleanedChar);
        }
        
        plateGray.release();
        plateBinary.release();
        hierarchy.release();
        
        return characters;
    }
    
    private static Mat cleanCharacter(Mat character) {
        Mat cleaned = new Mat();
        Imgproc.threshold(character, cleaned, 127, 255, Imgproc.THRESH_BINARY);
        
        Mat resized = new Mat();
        Size newSize = new Size(20, 20);
        Imgproc.resize(cleaned, resized, newSize);
        
        Mat result = new Mat(28, 28, CvType.CV_8UC1, new Scalar(255));
        Mat roi = result.submat(4, 24, 4, 24);
        resized.copyTo(roi);
        
        Core.bitwise_not(result, result);
        
        cleaned.release();
        resized.release();
        
        return result;
    }
    
    private static boolean isWhiteOnBlack(Mat binaryImage) {
        int borderPixels = 5;
        Mat topBorder = binaryImage.submat(0, borderPixels, 0, binaryImage.cols());
        Mat bottomBorder = binaryImage.submat(binaryImage.rows() - borderPixels, binaryImage.rows(), 0, binaryImage.cols());
        
        double topWhiteRatio = Core.countNonZero(topBorder) / (double) (topBorder.rows() * topBorder.cols());
        double bottomWhiteRatio = Core.countNonZero(bottomBorder) / (double) (bottomBorder.rows() * bottomBorder.cols());
        
        topBorder.release();
        bottomBorder.release();
        
        return (topWhiteRatio < 0.3 && bottomWhiteRatio < 0.3);
    }
    
    private static BufferedImage createBinaryCharactersImage(List<Mat> characters) {
        if (characters.isEmpty()) {
            return createPlaceholderImage(400, 100, "No characters detected");
        }
        
        int totalWidth = 0;
        int maxHeight = 0;
        
        for (Mat character : characters) {
            totalWidth += character.cols() + 2;
            maxHeight = Math.max(maxHeight, character.rows());
        }
        
        BufferedImage result = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, totalWidth, maxHeight);
        g2d.dispose();
        
        int x = 0;

        for (Mat character : characters) {
            BufferedImage charImage = matToBufferedImage(character);

            if (charImage != null) {
                Graphics2D g = result.createGraphics();
                g.drawImage(charImage, x, (maxHeight - charImage.getHeight()) / 2, null);
                g.dispose();
                x += charImage.getWidth() + 2;
            }
        }
        
        return result;
    }
    
    private static BufferedImage createRecognizedCharactersImage(List<Mat> characters) {
        if (characters.isEmpty()) {
            return createPlaceholderImage(400, 100, "No characters");
        }
        
        String recognizedText = recognizeCharacters(characters);
        
        int totalWidth = 400;
        int height = 100;
        
        BufferedImage result = new BufferedImage(totalWidth, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = result.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, totalWidth, height);
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(recognizedText);
        int xPos = (totalWidth - textWidth) / 2;
        int yPos = (height - fm.getHeight()) / 2 + fm.getAscent();
        
        g2d.drawString(recognizedText, xPos, yPos);
        g2d.dispose();
        
        return result;
    }
    
    private static String recognizeCharacters(List<Mat> characters) {
        if (characters.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        
        for (Mat character : characters) {
            char recognizedChar = recognizeCharacter(character);
            result.append(recognizedChar);
        }
        
        return result.toString();
    }
    
    private static char recognizeCharacter(Mat character) {
        int blackPixels = Core.countNonZero(character);
        int totalPixels = character.rows() * character.cols();
        double blackRatio = (double) blackPixels / totalPixels;
        
        if (blackRatio < 0.05) {
            return '?';
        }
        
        Rect boundingRect = getBoundingBox(character);
        double aspectRatio = (double) boundingRect.width / boundingRect.height;
        double fillRatio = (double) (boundingRect.width * boundingRect.height) / totalPixels;
        
        if (aspectRatio < 0.3) {
            return '1';
        }

        if (aspectRatio > 1.5) {
            return '-';
        }

        if (fillRatio > 0.8 && blackRatio > 0.6) {
            return '0';
        }

        if (blackRatio > 0.5) {
            return '8';
        }
        
        return 'A';
    }
    
    private static Rect getBoundingBox(Mat character) {
        Mat points = new Mat();
        Core.findNonZero(character, points);
        
        if (points.rows() == 0) {
            points.release();

            return new Rect(0, 0, character.cols(), character.rows());
        }
        
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        
        for (int i = 0; i < points.rows(); i++) {
            double[] point = points.get(i, 0);
            int x = (int) point[0];
            int y = (int) point[1];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        
        points.release();

        return new Rect(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
    
    private static void displayResultsTable() {
        imagePanel.removeAll();
        
        for (int i = 0; i < originalImages.size(); i++) {
            imagePanel.add(createImageLabel(originalImages.get(i), 400));
            
            if (i < plateROIs.size()) {
                imagePanel.add(createImageLabel(plateROIs.get(i), 400));
                imagePanel.add(createImageLabel(binaryChars.get(i), 400));
                imagePanel.add(createImageLabel(recognizedChars.get(i), 400));
            }
            else {
                imagePanel.add(new JLabel(" ", JLabel.CENTER));
                imagePanel.add(new JLabel(" ", JLabel.CENTER));
                imagePanel.add(new JLabel(" ", JLabel.CENTER));
            }
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static JLabel createImageLabel(BufferedImage image, int maxSize) {
        ImageIcon icon = new ImageIcon(scaleImageForDisplay(image, maxSize));
        JLabel label = new JLabel(icon);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        label.setPreferredSize(new Dimension(maxSize, maxSize));

        return label;
    }
    
    private static BufferedImage scaleImageForDisplay(BufferedImage original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return original;
        }
        
        double scale = Math.min((double) maxSize / width, (double) maxSize / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return scaled;
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
            int type = BufferedImage.TYPE_BYTE_GRAY;

            if (mat.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
                Mat temp = new Mat();
                Imgproc.cvtColor(mat, temp, Imgproc.COLOR_BGR2RGB);
                mat = temp;
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
            return createPlaceholderImage(400, 100, " ");
        }
    }
    
    private static void clearAll() {
        originalImages.clear();
        plateROIs.clear();
        binaryChars.clear();
        recognizedChars.clear();
        imagePanel.removeAll();
        imagePanel.revalidate();
        imagePanel.repaint();
    }
}
