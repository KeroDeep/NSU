import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static JLabel infoLabel;
    private static BufferedImage templateImage;
    private static BufferedImage sourceImage;
    private static BufferedImage transformedImage;
    private static List<org.opencv.core.Point> detectionResults = new ArrayList<>();
    private static FileManager currentFileManager = null;
    private static String sourceImagePath;
    
    private static double matchThreshold = 0.8;
    private static double brightness = 0;
    private static double contrast = 1.0;
    private static double noiseLevel = 0;
    private static double scale = 1.0;
    private static double rotation = 0;
    
    private static JSlider thresholdSlider;
    private static JSlider brightnessSlider;
    private static JSlider contrastSlider;
    private static JSlider noiseSlider;
    private static JSlider scaleSlider;
    private static JSlider rotationSlider;
    
    private static boolean showInitialMessage = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Object detection");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                imagePanel = new JPanel(new BorderLayout());
                imagePanel.setBackground(Color.WHITE);
                imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JScrollPane scrollPane = new JScrollPane(imagePanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                infoLabel = new JLabel(" ", JLabel.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                mainFrame.add(controlPanel, BorderLayout.NORTH);
                mainFrame.add(infoLabel, BorderLayout.CENTER);
                mainFrame.add(scrollPane, BorderLayout.CENTER);
                
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
                refreshDisplay();
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private static JPanel createControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel firstButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        firstButtonPanel.setBackground(mainPanel.getBackground());
        
        JButton loadSourceBtn = new JButton("Load source image");
        JButton loadTemplateBtn = new JButton("Load template image");
        JButton clearBtn = new JButton("Clear all");
        
        firstButtonPanel.add(loadSourceBtn);
        firstButtonPanel.add(loadTemplateBtn);
        firstButtonPanel.add(clearBtn);
        
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBackground(mainPanel.getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        thresholdSlider = new JSlider(50, 95, 80);
        thresholdSlider.setMajorTickSpacing(15);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        
        brightnessSlider = new JSlider(-100, 100, 0);
        brightnessSlider.setMajorTickSpacing(50);
        brightnessSlider.setMinorTickSpacing(10);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
        
        contrastSlider = new JSlider(10, 300, 100);
        contrastSlider.setMajorTickSpacing(100);
        contrastSlider.setMinorTickSpacing(25);
        contrastSlider.setPaintTicks(true);
        contrastSlider.setPaintLabels(true);
        
        noiseSlider = new JSlider(0, 100, 0);
        noiseSlider.setMajorTickSpacing(25);
        noiseSlider.setMinorTickSpacing(5);
        noiseSlider.setPaintTicks(true);
        noiseSlider.setPaintLabels(true);
        
        scaleSlider = new JSlider(50, 200, 100);
        scaleSlider.setMajorTickSpacing(50);
        scaleSlider.setMinorTickSpacing(10);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);
        
        rotationSlider = new JSlider(-180, 180, 0);
        rotationSlider.setMajorTickSpacing(90);
        rotationSlider.setMinorTickSpacing(30);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setPaintLabels(true);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Threshold:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(thresholdSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Brightness:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(brightnessSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Contrast:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(contrastSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Noise level:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(noiseSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Scale:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(scaleSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Rotation:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(rotationSlider, gbc);
        
        JPanel secondButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        secondButtonPanel.setBackground(mainPanel.getBackground());
        
        JButton resetBtn = new JButton("Reset parameters");
        JButton saveBtn = new JButton("Save result");
        
        secondButtonPanel.add(resetBtn);
        secondButtonPanel.add(saveBtn);
        
        loadSourceBtn.addActionListener(event -> openFileManagerForSource());
        loadTemplateBtn.addActionListener(event -> openFileManagerForTemplate());
        resetBtn.addActionListener(event -> resetParameters());
        saveBtn.addActionListener(event -> saveResult());
        clearBtn.addActionListener(event -> clearAll());
        
        thresholdSlider.addChangeListener(event -> {
            matchThreshold = thresholdSlider.getValue() / 100.0;

            if (sourceImage != null && templateImage != null) {
                performDetection();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        brightnessSlider.addChangeListener(event -> {
            brightness = brightnessSlider.getValue();

            if (sourceImage != null && templateImage != null) {
                performDetection();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        contrastSlider.addChangeListener(event -> {
            contrast = contrastSlider.getValue() / 100.0;

            if (sourceImage != null && templateImage != null) {
                performDetection();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        noiseSlider.addChangeListener(event -> {
            noiseLevel = noiseSlider.getValue();

            if (sourceImage != null && templateImage != null) {
                performDetection();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        scaleSlider.addChangeListener(event -> {
            scale = scaleSlider.getValue() / 100.0;

            if (sourceImage != null && templateImage != null) {
                performDetection();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        rotationSlider.addChangeListener(event -> {
            rotation = rotationSlider.getValue();

            if (sourceImage != null && templateImage != null) {
                performDetection();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        mainPanel.add(firstButtonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(paramsPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(secondButtonPanel);
        
        return mainPanel;
    }
    
    private static void openFileManagerForSource() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(paths -> {
            currentFileManager.close();
            currentFileManager = null;

            if (paths != null && paths.length > 0) {
                loadSourceImage(paths[0]);
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void openFileManagerForTemplate() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(paths -> {
            currentFileManager.close();
            currentFileManager = null;

            if (paths != null && paths.length > 0) {
                loadTemplateImage(paths[0]);
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadTemplateImage(String path) {
        try {
            File file = new File(path);
            templateImage = ImageIO.read(file);
            infoLabel.setText("Template loaded: " + file.getName());

            if (sourceImage != null) {
                performDetection();
            }

            refreshDisplay();
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error loading template", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void loadSourceImage(String path) {
        try {
            File file = new File(path);
            sourceImage = ImageIO.read(file);
            sourceImagePath = path;
            infoLabel.setText("Source image loaded: " + file.getName());

            if (templateImage != null) {
                performDetection();
            }

            refreshDisplay();
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error loading image", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void resetParameters() {
        thresholdSlider.setValue(80);
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        noiseSlider.setValue(0);
        scaleSlider.setValue(100);
        rotationSlider.setValue(0);
        
        matchThreshold = 0.8;
        brightness = 0;
        contrast = 1.0;
        noiseLevel = 0;
        scale = 1.0;
        rotation = 0;
        
        if (sourceImage != null && templateImage != null) {
            performDetection();
        }
    }
    
    private static void saveResult() {
        if (sourceImage == null || templateImage == null) {
            JOptionPane.showMessageDialog(mainFrame, "No result to save", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String currentDir = System.getProperty("user.dir");
            
            String originalFileName = "Source_image";

            if (sourceImagePath != null) {
                File sourceFile = new File(sourceImagePath);
                String fileName = sourceFile.getName();
                int dotIndex = fileName.lastIndexOf('.');

                if (dotIndex > 0) {
                    String nameWithoutExt = fileName.substring(0, dotIndex);
                    String extension = fileName.substring(dotIndex);
                    originalFileName = nameWithoutExt + "_result" + extension;
                }
                else {
                    originalFileName = fileName + "_result.png";
                }
            }
            else {
                originalFileName = "Detection_result.png";
            }
            
            File outputFile = new File(currentDir, originalFileName);
            
            BufferedImage resultImage = createResultImage();
            ImageIO.write(resultImage, "png", outputFile);
            JOptionPane.showMessageDialog(mainFrame, "Result saved as: " + outputFile.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving result: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static BufferedImage createResultImage() {
        if (detectionResults.isEmpty()) {
            return sourceImage;
        }
        
        BufferedImage resultImage = new BufferedImage(
            sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g2d = resultImage.createGraphics();
        g2d.drawImage(sourceImage, 0, 0, null);
        
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        
        int templateWidth = templateImage.getWidth();
        int templateHeight = templateImage.getHeight();
        
        for (org.opencv.core.Point point : detectionResults) {
            int x = (int) point.x;
            int y = (int) point.y;
            g2d.drawRect(x, y, templateWidth, templateHeight);
        }
        
        g2d.dispose();

        return resultImage;
    }
    
    private static void performDetection() {
        if (templateImage == null || sourceImage == null) {
            return;
        }
        
        try {
            detectionResults.clear();
            transformedImage = applyTransformations(sourceImage);
            
            Mat template = bufferedImageToMat(templateImage);
            Mat source = bufferedImageToMat(transformedImage);
            
            Mat templateGray = new Mat();
            Mat sourceGray = new Mat();
            
            if (template.channels() > 1) {
                Imgproc.cvtColor(template, templateGray, Imgproc.COLOR_BGR2GRAY);
            }
            else {
                templateGray = template.clone();
            }
            
            if (source.channels() > 1) {
                Imgproc.cvtColor(source, sourceGray, Imgproc.COLOR_BGR2GRAY);
            }
            else {
                sourceGray = source.clone();
            }
            
            Mat result = new Mat();
            Imgproc.matchTemplate(sourceGray, templateGray, result, Imgproc.TM_CCOEFF_NORMED);
            
            for (int i = 0; i < result.rows(); i++) {
                for (int j = 0; j < result.cols(); j++) {
                    double[] matchValue = result.get(i, j);

                    if (matchValue[0] >= matchThreshold) {
                        detectionResults.add(new org.opencv.core.Point(j, i));
                    }
                }
            }
            
            template.release();
            source.release();
            templateGray.release();
            sourceGray.release();
            result.release();
            
            infoLabel.setText(String.format("Detection completed: %d objects found", detectionResults.size()));
            refreshDisplay();
            
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Detection error: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static BufferedImage applyTransformations(BufferedImage image) {
        BufferedImage result = image;
        
        if (brightness != 0 || contrast != 1.0) {
            Mat mat = bufferedImageToMat(result);
            Mat adjusted = new Mat();
            mat.convertTo(adjusted, -1, contrast, brightness);
            result = matToBufferedImage(adjusted);
            mat.release();
            adjusted.release();
        }
        
        if (noiseLevel > 0) {
            Mat mat = bufferedImageToMat(result);
            Mat noise = new Mat(mat.size(), mat.type());
            Mat noisy = new Mat();
            Core.randn(noise, 0, noiseLevel);
            Core.add(mat, noise, noisy);
            result = matToBufferedImage(noisy);
            mat.release();
            noise.release();
            noisy.release();
        }
        
        if (scale != 1.0) {
            int newWidth = (int)(result.getWidth() * scale);
            int newHeight = (int)(result.getHeight() * scale);
            BufferedImage scaled = new BufferedImage(newWidth, newHeight, result.getType());
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(result, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            result = scaled;
        }
        
        if (rotation != 0) {
            double radians = Math.toRadians(rotation);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            
            int newWidth = (int) Math.floor(result.getWidth() * cos + result.getHeight() * sin);
            int newHeight = (int) Math.floor(result.getHeight() * cos + result.getWidth() * sin);
            
            BufferedImage rotated = new BufferedImage(newWidth, newHeight, result.getType());
            Graphics2D g2d = rotated.createGraphics();
            
            g2d.translate((newWidth - result.getWidth()) / 2, (newHeight - result.getHeight()) / 2);
            g2d.rotate(radians, result.getWidth() / 2, result.getHeight() / 2);
            g2d.drawRenderedImage(result, null);
            g2d.dispose();
            result = rotated;
        }
        
        return result;
    }
    
    private static void refreshDisplay() {
        imagePanel.removeAll();
        imagePanel.setLayout(new BorderLayout());
        
        if (showInitialMessage && (sourceImage == null || templateImage == null)) {
            JLabel messageLabel = new JLabel(
                "<html><div style='text-align: center;'>" +
                "Load source image and template to start detection<br>" +
                "</div></html>", 
                JLabel.CENTER
            );
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            messageLabel.setForeground(Color.BLACK);
            imagePanel.add(messageLabel, BorderLayout.CENTER);
        }
        else if (sourceImage != null || templateImage != null) {
            JPanel imagesPanel = new JPanel(new GridLayout(1, 4, 10, 10));
            imagesPanel.setBackground(Color.WHITE);
            imagesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            imagesPanel.add(createImagePanel(sourceImage, "Source image"));
            imagesPanel.add(createImagePanel(templateImage, "Template image"));
            
            if (sourceImage != null && transformedImage != null) {
                imagesPanel.add(createImagePanel(transformedImage, "Transformed image"));
            }
            else {
                imagesPanel.add(createPlaceholderPanel("No transformed image"));
            }
            
            if (sourceImage != null && templateImage != null) {
                imagesPanel.add(createResultPanel());
            }
            else {
                imagesPanel.add(createPlaceholderPanel("Load both images for detection"));
            }
            
            imagePanel.add(imagesPanel, BorderLayout.CENTER);
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static JPanel createImagePanel(BufferedImage image, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        if (image != null) {
            JLabel imageLabel = new JLabel(new ImageIcon(scaleImageForDisplay(image, 300)));
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            panel.add(imageLabel, BorderLayout.CENTER);
        }
        else {
            JLabel placeholder = createTextLabel("Not loaded");
            panel.add(placeholder, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    private static JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Detection result"));
        
        BufferedImage resultImage;
        
        if (detectionResults.isEmpty()) {
            resultImage = sourceImage;
        }
        else {
            resultImage = new BufferedImage(
                sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g2d = resultImage.createGraphics();
            g2d.drawImage(sourceImage, 0, 0, null);
            
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            
            int templateWidth = templateImage.getWidth();
            int templateHeight = templateImage.getHeight();
            
            for (org.opencv.core.Point point : detectionResults) {
                int x = (int) point.x;
                int y = (int) point.y;
                g2d.drawRect(x, y, templateWidth, templateHeight);
            }
            
            g2d.dispose();
        }
        
        JLabel imageLabel = new JLabel(new ImageIcon(scaleImageForDisplay(resultImage, 300)));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        
        panel.add(imageLabel, BorderLayout.CENTER);

        return panel;
    }
    
    private static JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Detection result"));
        
        JLabel label = createTextLabel(message);
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static JLabel createTextLabel(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setForeground(Color.BLACK);
        label.setFont(new Font("Arial", Font.PLAIN, 14));

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
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, mob);
            byte[] byteArray = mob.toArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
            BufferedImage image = ImageIO.read(bis);
            bis.close();
            mob.release();

            return image;
        }
        catch (Exception exception) {
            return null;
        }
    }
    
    private static void clearAll() {
        templateImage = null;
        sourceImage = null;
        transformedImage = null;
        sourceImagePath = null;
        detectionResults.clear();
        showInitialMessage = false;
        
        thresholdSlider.setValue(80);
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        noiseSlider.setValue(0);
        scaleSlider.setValue(100);
        rotationSlider.setValue(0);
        
        matchThreshold = 0.8;
        brightness = 0;
        contrast = 1.0;
        noiseLevel = 0;
        scale = 1.0;
        rotation = 0;
        
        infoLabel.setText(" ");
        refreshDisplay();
    }
}
