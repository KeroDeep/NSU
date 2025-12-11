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
    
    private static double watershedThreshold = 0.5;
    private static int graphCutIterations = 1;
    
    private static JSlider watershedThresholdSlider;
    private static JSlider graphCutIterationsSlider;
    
    private static boolean isProcessing = false;
    private static javax.swing.Timer updateTimer;

    public static void main(String[] args) {
        updateTimer = new javax.swing.Timer(300, event -> refreshDisplay());
        updateTimer.setRepeats(false);
        
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Image segmentation");
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
                JOptionPane.showMessageDialog(null, "Error: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static JPanel createControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(mainPanel.getBackground());
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setMaximumSize(new Dimension(600, 50));
        
        JButton openBtn = new JButton("Open file manager");
        JButton clearBtn = new JButton("Clear All");
        
        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        
        buttonPanel.add(openBtn);
        buttonPanel.add(clearBtn);
        
        JPanel watershedPanel = createControlRow("Watershed threshold:", 0, 100, (int)(watershedThreshold * 100));
        JPanel graphCutPanel = createControlRow("Graph cut iterations:", 1, 10, graphCutIterations);
        
        watershedThresholdSlider = (JSlider) watershedPanel.getClientProperty("slider");
        graphCutIterationsSlider = (JSlider) graphCutPanel.getClientProperty("slider");
        
        watershedThresholdSlider.addChangeListener(event -> {
            watershedThreshold = watershedThresholdSlider.getValue() / 100.0;
            updateTimer.restart();
        });
        
        graphCutIterationsSlider.addChangeListener(event -> {
            graphCutIterations = graphCutIterationsSlider.getValue();
            updateTimer.restart();
        });
        
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(watershedPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(graphCutPanel);
        
        return mainPanel;
    }
    
    private static JPanel createControlRow(String label, int min, int max, int value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setMaximumSize(new Dimension(600, 60));
        
        JLabel textLabel = new JLabel(label);
        textLabel.setPreferredSize(new Dimension(150, 25));
        textLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        JSlider slider = new JSlider(min, max, value);
        
        if (min == 0 && max == 100) {
            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(5);
        }
        else {
            slider.setMajorTickSpacing(1);
            slider.setMinorTickSpacing(1);
        }
        
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(350, 50));
        
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        sliderPanel.setBackground(panel.getBackground());
        sliderPanel.add(slider);
        
        panel.add(textLabel, BorderLayout.WEST);
        panel.add(sliderPanel, BorderLayout.CENTER);
        
        panel.putClientProperty("slider", slider);
        
        return panel;
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
                refreshDisplay();
            }
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
                    originalImages.add(image);
                    imagePaths.add(path);
                }
            }
            catch (Exception event) {
                System.err.println("Error loading image: " + path);
            }
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
        if (isProcessing) {
            return;
        }
        
        isProcessing = true;
        
        new Thread(() -> {
            try {
                final List<JPanel> resultPanels = new ArrayList<>();
                
                for (int i = 0; i < originalImages.size(); i++) {
                    BufferedImage original = originalImages.get(i);
                    
                    BufferedImage watershedResult = applyWatershed(original);
                    BufferedImage graphCutResult = applyGraphCut(original);
                    
                    JPanel imageRowPanel = createImageRow(original, watershedResult, graphCutResult);
                    resultPanels.add(imageRowPanel);
                }
                
                SwingUtilities.invokeLater(() -> {
                    imagePanel.removeAll();
                    
                    if (originalImages.isEmpty()) {
                        addLabel("<html>No images loaded. Click &laquo;Open file manager&raquo; to load images.</html>");
                    }
                    else {
                        for (JPanel panel : resultPanels) {
                            imagePanel.add(panel);
                            imagePanel.add(Box.createRigidArea(new Dimension(0, 20)));
                        }
                    }
                    
                    imagePanel.revalidate();
                    imagePanel.repaint();
                    isProcessing = false;
                });
                
            }
            catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    imagePanel.removeAll();
                    addLabel("Error processing images");
                    imagePanel.revalidate();
                    imagePanel.repaint();
                    isProcessing = false;
                });
            }
        }).start();
    }

    private static JPanel createImageRow(BufferedImage original, BufferedImage watershedResult, BufferedImage graphCutResult) {
        JPanel imageRowPanel = new JPanel(new BorderLayout());
        imageRowPanel.setBackground(Color.WHITE);
        imageRowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel comparisonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        comparisonPanel.setBackground(Color.WHITE);
        
        addImageToPanel(comparisonPanel, original, "Original image");
        addImageToPanel(comparisonPanel, watershedResult, "Watershed result");
        addImageToPanel(comparisonPanel, graphCutResult, "Graph cut result");
        
        imageRowPanel.add(comparisonPanel, BorderLayout.CENTER);

        return imageRowPanel;
    }
    
    private static BufferedImage applyWatershed(BufferedImage original) {
        BufferedImage workingImage = original;

        if (original.getWidth() > 600 || original.getHeight() > 600) {
            workingImage = scaleImageForDisplay(original, 600);
        }
        
        Mat originalMat = bufferedImageToMat(workingImage);

        if (originalMat.empty()) {
            return workingImage;
        }
        
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(originalMat, gray, Imgproc.COLOR_BGR2GRAY);
            
            Mat binary = new Mat();
            Imgproc.threshold(gray, binary, watershedThreshold * 255, 255, Imgproc.THRESH_BINARY);
            
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Mat opening = new Mat();
            Imgproc.morphologyEx(binary, opening, Imgproc.MORPH_OPEN, kernel, new org.opencv.core.Point(-1, -1), 2);
            
            Mat sureBg = new Mat();
            Imgproc.dilate(opening, sureBg, kernel, new org.opencv.core.Point(-1, -1), 3);
            
            Mat distTransform = new Mat();
            Imgproc.distanceTransform(opening, distTransform, Imgproc.DIST_L2, 5);
            
            Mat sureFg = new Mat();
            Imgproc.threshold(distTransform, sureFg, 0.3 * watershedThreshold * 255, 255, Imgproc.THRESH_BINARY);
            
            sureFg.convertTo(sureFg, CvType.CV_8U);
            
            Mat unknown = new Mat();
            Core.subtract(sureBg, sureFg, unknown);
            
            Mat markers = new Mat();
            Imgproc.connectedComponents(sureFg, markers);
            
            for (int i = 0; i < markers.rows(); i++) {
                for (int j = 0; j < markers.cols(); j++) {
                    if (unknown.get(i, j)[0] == 255) {
                        markers.put(i, j, 0);
                    }
                }
            }
            
            markers.convertTo(markers, CvType.CV_32S);
            Mat resultMat = originalMat.clone();
            Imgproc.watershed(resultMat, markers);
            
            Mat result = new Mat(originalMat.size(), CvType.CV_8UC3);

            for (int i = 0; i < markers.rows(); i++) {
                for (int j = 0; j < markers.cols(); j++) {
                    int index = (int) markers.get(i, j)[0];

                    if (index == -1) {
                        for (int k = -1; k <= 1; k++) {
                            for (int l = -1; l <= 1; l++) {
                                int x = j + k;
                                int y = i + l;

                                if (x >= 0 && x < result.cols() && y >= 0 && y < result.rows()) {
                                    result.put(y, x, 0, 0, 255);
                                }
                            }
                        }
                    }
                    else {
                        double[] color = originalMat.get(i, j);
                        result.put(i, j, color);
                    }
                }
            }
            
            BufferedImage resultImage = matToBufferedImage(result);
            
            originalMat.release();
            gray.release();
            binary.release();
            kernel.release();
            opening.release();
            sureBg.release();
            distTransform.release();
            sureFg.release();
            unknown.release();
            markers.release();
            result.release();
            resultMat.release();
            
            return resultImage;
        }
        catch (Exception exception) {
            return workingImage;
        }
    }
    
    private static BufferedImage applyGraphCut(BufferedImage original) {
        BufferedImage workingImage = original;

        if (original.getWidth() > 600 || original.getHeight() > 600) {
            workingImage = scaleImageForDisplay(original, 600);
        }
        
        Mat originalMat = bufferedImageToMat(workingImage);

        if (originalMat.empty()) {
            return workingImage;
        }
        
        try {
            Mat mask = new Mat(originalMat.size(), CvType.CV_8UC1, new Scalar(0));
            
            int marginX = originalMat.cols() / 6;
            int marginY = originalMat.rows() / 6;
            Rect rectangle = new Rect(marginX, marginY, originalMat.cols() - 2 * marginX, originalMat.rows() - 2 * marginY);
            
            Mat bgdModel = new Mat();
            Mat fgdModel = new Mat();
            
            Imgproc.grabCut(originalMat, mask, rectangle, bgdModel, fgdModel, graphCutIterations, Imgproc.GC_INIT_WITH_RECT);
            
            Mat resultMask = new Mat();
            Mat fgMask = new Mat();
            Mat prFgMask = new Mat();
            
            Core.compare(mask, new Scalar(Imgproc.GC_FGD), fgMask, Core.CMP_EQ);
            Core.compare(mask, new Scalar(Imgproc.GC_PR_FGD), prFgMask, Core.CMP_EQ);
            
            Core.bitwise_or(fgMask, prFgMask, resultMask);
            
            Mat result = new Mat(originalMat.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
            originalMat.copyTo(result, resultMask);
            
            BufferedImage resultImage = matToBufferedImage(result);
            
            originalMat.release();
            mask.release();
            bgdModel.release();
            fgdModel.release();
            resultMask.release();
            fgMask.release();
            prFgMask.release();
            result.release();
            
            return resultImage;
        }
        catch (Exception exception) {
            return workingImage;
        }
    }
    
    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 2), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        BufferedImage scaledImage = scaleImageForDisplay(image, 350);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        
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
            if (mat.empty()) {
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            }
            
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
