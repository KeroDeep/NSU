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
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
    private static int lowerThreshold = 50;
    private static int upperThreshold = 150;

    private static JSlider threshold1Slider;
    private static JSlider threshold2Slider;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Automatic object size analysis");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(1400, 900);
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

        threshold1Slider = new JSlider(0, 255, lowerThreshold);
        threshold1Slider.setMajorTickSpacing(50);
        threshold1Slider.setMinorTickSpacing(10);
        threshold1Slider.setPaintTicks(true);
        threshold1Slider.setPaintLabels(true);

        threshold2Slider = new JSlider(0, 255, upperThreshold);
        threshold2Slider.setMajorTickSpacing(50);
        threshold2Slider.setMinorTickSpacing(10);
        threshold2Slider.setPaintTicks(true);
        threshold2Slider.setPaintLabels(true);

        threshold1Slider.addChangeListener(event -> {
            lowerThreshold = threshold1Slider.getValue();

            if (lowerThreshold >= upperThreshold) {
                upperThreshold = Math.min(255, lowerThreshold + 1);
                threshold2Slider.setValue(upperThreshold);
            }

            refreshDisplay();
        });

        threshold2Slider.addChangeListener(event -> {
            upperThreshold = threshold2Slider.getValue();

            if (upperThreshold <= lowerThreshold) {
                lowerThreshold = Math.max(0, upperThreshold - 1);
                threshold1Slider.setValue(lowerThreshold);
            }

            refreshDisplay();
        });

        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        resetBtn.addActionListener(event -> resetParameters());

        buttonPanel.add(openBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(resetBtn);

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(mainPanel.getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Lower threshold:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 4;
        controlPanel.add(threshold1Slider, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Upper threshold:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 4;
        controlPanel.add(threshold2Slider, gbc);

        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(controlPanel);

        return mainPanel;
    }

    private static void resetParameters() {
        lowerThreshold = 50;
        upperThreshold = 150;
        
        threshold1Slider.setValue(lowerThreshold);
        threshold2Slider.setValue(upperThreshold);
        
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
                        BufferedImage scaledImage = scaleImageForProcessing(colorImage, 600, 400);
                        BufferedImage grayImage = convertToGrayscale(scaledImage);
                        originalColorImages.add(scaledImage);
                        originalGrayImages.add(grayImage);
                        imagePaths.add(path);
                    }
                }
            }
            catch (Exception event) {
                System.err.println("Error loading image: " + path);
                event.printStackTrace();
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
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

            BufferedImage colorOriginal = originalColorImages.get(i);
            BufferedImage grayOriginal = originalGrayImages.get(i);
            
            Object[] result = analyzeObjectSizeEnhanced(colorOriginal, grayOriginal);
            
            addImageToPanel(rowPanel, colorOriginal, "Original");
            addImageToPanel(rowPanel, grayOriginal, "Grayscale");
            addImageToPanel(rowPanel, (BufferedImage) result[1], "Detected objects");
            
            String overlayTitle = String.format("Overlay - %s", result[2]);
            addImageToPanel(rowPanel, (BufferedImage) result[0], overlayTitle);
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
    }

    private static Object[] analyzeObjectSizeEnhanced(BufferedImage colorOriginal, BufferedImage grayOriginal) {
        Mat grayMat = bufferedImageToGrayMat(grayOriginal);
        Mat colorMat = bufferedImageToMat(colorOriginal);
        
        List<Mat> segmentationResults = new ArrayList<>();
        
        Mat adaptiveThresh = new Mat();
        Imgproc.adaptiveThreshold(grayMat, adaptiveThresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        segmentationResults.add(adaptiveThresh);
        
        Mat adaptiveThreshInv = new Mat();
        Imgproc.adaptiveThreshold(grayMat, adaptiveThreshInv, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
        segmentationResults.add(adaptiveThreshInv);
        
        Mat otsuThresh = new Mat();
        Imgproc.threshold(grayMat, otsuThresh, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        segmentationResults.add(otsuThresh);
        
        Mat otsuThreshInv = new Mat();
        Imgproc.threshold(grayMat, otsuThreshInv, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        segmentationResults.add(otsuThreshInv);
        
        Mat simpleThresh = new Mat();
        Imgproc.threshold(grayMat, simpleThresh, lowerThreshold, 255, Imgproc.THRESH_BINARY);
        segmentationResults.add(simpleThresh);
        
        Mat simpleThreshInv = new Mat();
        Imgproc.threshold(grayMat, simpleThreshInv, lowerThreshold, 255, Imgproc.THRESH_BINARY_INV);
        segmentationResults.add(simpleThreshInv);
        
        Mat cannyEdges = new Mat();
        Imgproc.Canny(grayMat, cannyEdges, lowerThreshold, upperThreshold, 3, false);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(cannyEdges, cannyEdges, Imgproc.MORPH_CLOSE, kernel);
        segmentationResults.add(cannyEdges);
        
        Mat bestSegmentation = selectBestSegmentationForAllSizes(segmentationResults, grayMat);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(bestSegmentation, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        Mat overlay = colorMat.clone();
        Mat objectsImage = Mat.zeros(grayMat.size(), CvType.CV_8UC3);
        
        String sizeCategory = "No objects";
        double totalObjectArea = 0;
        int imageArea = colorOriginal.getWidth() * colorOriginal.getHeight();
        
        List<MatOfPoint> filteredContours = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            Rect rect = Imgproc.boundingRect(contour);
            
            double minArea = Math.max(5, imageArea * 0.0001);
            double maxArea = imageArea * 0.99;
            
            if (area < minArea || area > maxArea) {
                continue;
            }
            
            double width = rect.width;
            double height = rect.height;
            double aspectRatio = Math.max(width / height, height / width);
            
            if (aspectRatio > 50) {
                continue;
            }
            
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double perimeter = Imgproc.arcLength(contour2f, true);
            double compactness = 0;

            if (perimeter > 0) {
                compactness = (4 * Math.PI * area) / (perimeter * perimeter);
            }
            
            boolean isValid = false;
            if (area < imageArea * 0.01) {
                isValid = compactness > 0.02 || area < imageArea * 0.005;
            }
            else if (area < imageArea * 0.3) {
                isValid = compactness > 0.03 || aspectRatio < 10;
            }
            else {
                isValid = compactness > 0.01 || aspectRatio < 5;
            }
            
            if (isValid) {
                filteredContours.add(contour);
                totalObjectArea += area;
            }

            contour2f.release();
        }
        
        Scalar white = new Scalar(255, 255, 255);
        Scalar red = new Scalar(0, 0, 255);
        Scalar green = new Scalar(0, 255, 0);
        Scalar blue = new Scalar(255, 0, 0);
        
        Imgproc.drawContours(objectsImage, filteredContours, -1, white, -1);
        
        for (MatOfPoint contour : filteredContours) {
            Rect rect = Imgproc.boundingRect(contour);
            Imgproc.rectangle(overlay, rect.tl(), rect.br(), red, 2);
            
            double area = Imgproc.contourArea(contour);
            double areaPercentage = (area / imageArea) * 100;
            String text = String.format("%.1f%%", areaPercentage);
            Imgproc.putText(overlay, text, new Point(rect.x, rect.y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, green, 1);
        }
        
        if (filteredContours.size() > 0) {
            double areaPercentage = (totalObjectArea / imageArea) * 100;
            
            if (areaPercentage < 5) {
                sizeCategory = "Small (" + String.format("%.1f", areaPercentage) + "%)";
            }
            else if (areaPercentage < 40) {
                sizeCategory = "Medium (" + String.format("%.1f", areaPercentage) + "%)";
            }
            else {
                sizeCategory = "Large (" + String.format("%.1f", areaPercentage) + "%)";
            }
            
            String sizeText = String.format("Total: %.1f%%", areaPercentage);
            Imgproc.putText(overlay, sizeText, new Point(10, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, blue, 2);
            
            String objectsText = String.format("Objects: %d", filteredContours.size());
            Imgproc.putText(overlay, objectsText, new Point(10, 60), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, blue, 2);
        }
        
        BufferedImage overlayBuffered = matToBufferedImage(overlay);
        BufferedImage objectsBuffered = matToBufferedImage(objectsImage);
        
        grayMat.release();
        colorMat.release();
        hierarchy.release();
        overlay.release();
        objectsImage.release();
        bestSegmentation.release();
        kernel.release();

        for (Mat mat : segmentationResults) {
            mat.release();
        }
        
        return new Object[]{overlayBuffered, objectsBuffered, sizeCategory};
    }

    private static Mat selectBestSegmentationForAllSizes(List<Mat> segmentations, Mat grayMat) {
        int totalPixels = grayMat.rows() * grayMat.cols();
        Mat bestOverall = segmentations.get(0);
        double bestOverallScore = -1;
        
        for (Mat segmentation : segmentations) {
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(segmentation, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            
            int validContours = 0;
            double totalValidArea = 0;
            
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);

                if (area > totalPixels * 0.0001 && area < totalPixels * 0.99) {
                    validContours++;
                    totalValidArea += area;
                }
            }
            
            double score = 0;

            if (validContours > 0) {
                double avgArea = totalValidArea / validContours;
                double areaScore = 1.0 - Math.min(Math.abs(avgArea - totalPixels * 0.1) / (totalPixels * 0.1), 1.0);
                double contourScore = Math.min(validContours / 5.0, 1.0);
                score = areaScore * contourScore * validContours;
            }
            
            if (score > bestOverallScore) {
                bestOverallScore = score;
                bestOverall = segmentation;
            }
            
            hierarchy.release();
        }
        
        return bestOverall;
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
        JLabel imageLabel = new JLabel(new ImageIcon(scaleImage(image, 250, 200)));
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
