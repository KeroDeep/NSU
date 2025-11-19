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
    private static BufferedImage sourceImage;
    private static BufferedImage processedImage;
    private static String detectedTime = "Not detected";
    private static String detectedScore = "Not detected";
    private static FileManager currentFileManager = null;
    private static String sourceImagePath;
    
    private static double brightness = 0;
    private static double contrast = 1.0;
    private static double thresholdValue = 128;
    private static double scale = 1.0;
    
    private static JSlider brightnessSlider;
    private static JSlider contrastSlider;
    private static JSlider thresholdSlider;
    private static JSlider scaleSlider;
    
    private static boolean showInitialMessage = false;
    
    private static Map<String, Mat> timeDigitTemplates = new HashMap<>();
    private static Map<String, Mat> scoreDigitTemplates = new HashMap<>();

    public static void main(String[] args) {
        loadDigitTemplates();
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Time and score detection");
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
    
    private static void loadDigitTemplates() {
        for (int i = 0; i <= 9; i++) {
            String timeTemplatePath = "./Templates/Time_" + i + ".jpg";
            File timeTemplateFile = new File(timeTemplatePath);

            if (timeTemplateFile.exists()) {
                try {
                    BufferedImage templateImage = ImageIO.read(timeTemplateFile);
                    Mat templateMat = bufferedImageToMat(templateImage);
                    Mat grayTemplate = new Mat();

                    if (templateMat.channels() > 1) {
                        Imgproc.cvtColor(templateMat, grayTemplate, Imgproc.COLOR_BGR2GRAY);
                    }
                    else {
                        grayTemplate = templateMat.clone();
                    }

                    timeDigitTemplates.put(String.valueOf(i), grayTemplate);
                    templateMat.release();
                }
                catch (Exception exception) {}
            }
        }
        
        for (int i = 0; i <= 9; i++) {
            String scoreTemplatePath = "./Templates/Score_" + i + ".jpg";
            File scoreTemplateFile = new File(scoreTemplatePath);

            if (scoreTemplateFile.exists()) {
                try {
                    BufferedImage templateImage = ImageIO.read(scoreTemplateFile);
                    Mat templateMat = bufferedImageToMat(templateImage);
                    Mat grayTemplate = new Mat();

                    if (templateMat.channels() > 1) {
                        Imgproc.cvtColor(templateMat, grayTemplate, Imgproc.COLOR_BGR2GRAY);
                    }
                    else {
                        grayTemplate = templateMat.clone();
                    }

                    scoreDigitTemplates.put(String.valueOf(i), grayTemplate);
                    templateMat.release();
                }
                catch (Exception exception) {}
            }
        }
    }

    private static JPanel createControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel firstButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        firstButtonPanel.setBackground(mainPanel.getBackground());
        
        JButton loadImageBtn = new JButton("Load screenshot");
        JButton resetBtn = new JButton("Reset parameters");
        JButton clearBtn = new JButton("Clear all");
        
        firstButtonPanel.add(loadImageBtn);
        firstButtonPanel.add(resetBtn);
        firstButtonPanel.add(clearBtn);
        
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBackground(mainPanel.getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        brightnessSlider = new JSlider(-255, 255, 0);
        brightnessSlider.setMajorTickSpacing(100);
        brightnessSlider.setMinorTickSpacing(25);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
        
        contrastSlider = new JSlider(0, 300, 100);
        contrastSlider.setMajorTickSpacing(100);
        contrastSlider.setMinorTickSpacing(25);
        contrastSlider.setPaintTicks(true);
        contrastSlider.setPaintLabels(true);
        
        thresholdSlider = new JSlider(0, 255, 128);
        thresholdSlider.setMajorTickSpacing(50);
        thresholdSlider.setMinorTickSpacing(10);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        
        scaleSlider = new JSlider(50, 200, 100);
        scaleSlider.setMajorTickSpacing(50);
        scaleSlider.setMinorTickSpacing(10);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Brightness:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(brightnessSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Contrast:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(contrastSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Threshold:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(thresholdSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        paramsPanel.add(new JLabel("Scale:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        paramsPanel.add(scaleSlider, gbc);
        
        loadImageBtn.addActionListener(event -> openFileManager());
        resetBtn.addActionListener(event -> resetParameters());
        clearBtn.addActionListener(event -> clearAll());
        
        brightnessSlider.addChangeListener(event -> {
            brightness = brightnessSlider.getValue();

            if (sourceImage != null) {
                processImage();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        contrastSlider.addChangeListener(event -> {
            contrast = contrastSlider.getValue() / 100.0;

            if (sourceImage != null) {
                processImage();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        thresholdSlider.addChangeListener(event -> {
            thresholdValue = thresholdSlider.getValue();

            if (sourceImage != null) {
                processImage();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        scaleSlider.addChangeListener(event -> {
            scale = scaleSlider.getValue() / 100.0;

            if (sourceImage != null) {
                processImage();
            }
            else {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        mainPanel.add(firstButtonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(paramsPanel);
        
        return mainPanel;
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
                loadImage(paths[0]);
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadImage(String path) {
        try {
            File file = new File(path);
            sourceImage = ImageIO.read(file);
            sourceImagePath = path;
            processImage();
            refreshDisplay();
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error loading image", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void processImage() {
        if (sourceImage == null) return;
        
        try {
            processedImage = applyTransformations(sourceImage);
            detectMatchTimeAndScore();
            refreshDisplay();
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Processing error", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void detectMatchTimeAndScore() {
        if (processedImage == null) return;
        
        try {
            Mat imageMat = bufferedImageToMat(processedImage);
            
            String timeResult = findTimeWithTemplateMatching(imageMat);
            detectedTime = formatTime(timeResult);
            
            String scoreResult = findScoreWithTemplateMatching(imageMat);
            detectedScore = formatScore(scoreResult);
            
            imageMat.release();
            
        }
        catch (Exception exception) {
            detectedTime = "Recognition error";
            detectedScore = "Recognition error";
        }
    }
    
    private static String findTimeWithTemplateMatching(Mat image) {
        List<Object[]> timeMatches = new ArrayList<>();
        
        Rect timeROI = new Rect(675, 40, 150, 60);
        
        if (timeROI.x < 0 || timeROI.y < 0 || timeROI.x + timeROI.width > image.cols() || timeROI.y + timeROI.height > image.rows()) {
            return "";
        }
        
        Mat timeRegion = new Mat(image, timeROI);
        Mat gray = new Mat();
        Imgproc.cvtColor(timeRegion, gray, Imgproc.COLOR_BGR2GRAY);
        
        for (Map.Entry<String, Mat> entry : timeDigitTemplates.entrySet()) {
            String digit = entry.getKey();
            Mat template = entry.getValue();
            
            if (template.rows() > timeRegion.rows() || template.cols() > timeRegion.cols()) {
                continue;
            }
            
            Mat result = new Mat();
            Imgproc.matchTemplate(gray, template, result, Imgproc.TM_CCOEFF_NORMED);
            
            for (int i = 0; i < result.rows(); i++) {
                for (int j = 0; j < result.cols(); j++) {
                    double[] matchValue = result.get(i, j);

                    if (matchValue[0] >= 0.5) {
                        timeMatches.add(new Object[]{j, i, digit, matchValue[0]});
                    }
                }
            }
            result.release();
        }
        
        gray.release();
        timeRegion.release();
        
        Collections.sort(timeMatches, (a, b) -> Integer.compare((Integer)a[0], (Integer)b[0]));
        
        List<Object[]> filtered = filterOverlappingMatches(timeMatches, 15);
        
        StringBuilder timeBuilder = new StringBuilder();

        for (Object[] match : filtered) {
            timeBuilder.append((String)match[2]);
        }
        
        return timeBuilder.toString();
    }
    
    private static String findScoreWithTemplateMatching(Mat image) {
        List<Object[]> leftScoreMatches = new ArrayList<>();
        List<Object[]> rightScoreMatches = new ArrayList<>();
        
        Rect leftScoreROI = new Rect(570, 10, 100, 90);
        Rect rightScoreROI = new Rect(780, 10, 130, 90);
        
        if (leftScoreROI.x < 0 || leftScoreROI.y < 0 || leftScoreROI.x + leftScoreROI.width > image.cols() || leftScoreROI.y + leftScoreROI.height > image.rows()) {
            return "";
        }
        
        if (rightScoreROI.x < 0 || rightScoreROI.y < 0 || rightScoreROI.x + rightScoreROI.width > image.cols() || rightScoreROI.y + rightScoreROI.height > image.rows()) {
            return "";
        }
        
        Mat leftScoreRegion = new Mat(image, leftScoreROI);
        Mat rightScoreRegion = new Mat(image, rightScoreROI);
        
        Mat leftGray = new Mat();
        Mat rightGray = new Mat();
        Imgproc.cvtColor(leftScoreRegion, leftGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(rightScoreRegion, rightGray, Imgproc.COLOR_BGR2GRAY);
        
        for (Map.Entry<String, Mat> entry : scoreDigitTemplates.entrySet()) {
            String digit = entry.getKey();
            Mat template = entry.getValue();
            
            if (template.rows() > leftScoreRegion.rows() || template.cols() > leftScoreRegion.cols()) {
                continue;
            }
            
            Mat leftResult = new Mat();
            Imgproc.matchTemplate(leftGray, template, leftResult, Imgproc.TM_CCOEFF_NORMED);
            
            for (int i = 0; i < leftResult.rows(); i++) {
                for (int j = 0; j < leftResult.cols(); j++) {
                    double[] matchValue = leftResult.get(i, j);

                    if (matchValue[0] >= 0.5) {
                        leftScoreMatches.add(new Object[]{j, i, digit, matchValue[0]});
                    }
                }
            }
            leftResult.release();
            
            Mat rightResult = new Mat();
            Imgproc.matchTemplate(rightGray, template, rightResult, Imgproc.TM_CCOEFF_NORMED);
            
            for (int i = 0; i < rightResult.rows(); i++) {
                for (int j = 0; j < rightResult.cols(); j++) {
                    double[] matchValue = rightResult.get(i, j);

                    if (matchValue[0] >= 0.5) {
                        rightScoreMatches.add(new Object[]{j, i, digit, matchValue[0]});
                    }
                }
            }
            
            rightResult.release();
        }
        
        leftGray.release();
        rightGray.release();
        leftScoreRegion.release();
        rightScoreRegion.release();
        
        Collections.sort(leftScoreMatches, (a, b) -> Integer.compare((Integer)a[0], (Integer)b[0]));
        Collections.sort(rightScoreMatches, (a, b) -> Integer.compare((Integer)a[0], (Integer)b[0]));
        
        List<Object[]> filteredLeft = filterOverlappingMatches(leftScoreMatches, 12);
        List<Object[]> filteredRight = filterOverlappingMatches(rightScoreMatches, 12);
        
        StringBuilder leftScore = new StringBuilder();
        StringBuilder rightScore = new StringBuilder();

        for (Object[] match : filteredLeft) {
            leftScore.append((String)match[2]);
        }
        
        for (Object[] match : filteredRight) {
            rightScore.append((String)match[2]);
        }
        
        return leftScore.toString() + ":" + rightScore.toString();
    }
    
    private static int findScoreGap(List<Object[]> matches) {
        if (matches.size() < 2) {
            return 0;
        }
        
        int maxGapIndex = 0;
        double maxGap = 0;
        
        for (int i = 0; i < matches.size() - 1; i++) {
            int currentX = (Integer)matches.get(i)[0];
            int nextX = (Integer)matches.get(i + 1)[0];
            double gap = nextX - currentX;
            
            if (gap > maxGap && gap > 30) {
                maxGap = gap;
                maxGapIndex = i;
            }
        }
        
        return maxGapIndex;
    }
    
    private static List<Object[]> filterOverlappingMatches(List<Object[]> matches, int minDistance) {
        List<Object[]> filtered = new ArrayList<>();
        boolean[] used = new boolean[matches.size()];
        
        for (int i = 0; i < matches.size(); i++) {
            if (used[i]) {
                continue;
            }
            
            Object[] current = matches.get(i);
            filtered.add(current);
            used[i] = true;
            
            for (int j = i + 1; j < matches.size(); j++) {
                if (used[j]) {
                    continue;
                }
                
                Object[] other = matches.get(j);
                int currentX = (Integer)current[0];
                int currentY = (Integer)current[1];
                int otherX = (Integer)other[0];
                int otherY = (Integer)other[1];
                double distance = Math.sqrt(Math.pow(currentX - otherX, 2) + Math.pow(currentY - otherY, 2));
                
                if (distance < minDistance) {
                    double currentConfidence = (Double)current[3];
                    double otherConfidence = (Double)other[3];

                    if (otherConfidence > currentConfidence) {
                        filtered.remove(filtered.size() - 1);
                        filtered.add(other);
                        current = other;
                    }

                    used[j] = true;
                }
            }
        }
        
        return filtered;
    }
    
    private static String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) {
            return "Time not found";
        }
        
        String digitsOnly = rawTime.replaceAll("[^0-9]", "");
        
        switch (digitsOnly.length()) {
            case 6: return digitsOnly.substring(0, 2) + ":" + digitsOnly.substring(2, 4) + ":" + digitsOnly.substring(4, 6);
            case 5: return "0" + digitsOnly.substring(0, 1) + ":" + digitsOnly.substring(1, 3) + ":" + digitsOnly.substring(3, 5);
            case 4: return "00:" + digitsOnly.substring(0, 2) + ":" + digitsOnly.substring(2, 4);
            case 3: return "00:0" + digitsOnly.substring(0, 1) + ":" + digitsOnly.substring(1, 3);
            case 2: return "00:00:" + digitsOnly;
            case 1: return "00:00:0" + digitsOnly;
            default: return "Time not found";
        }
    }
    
    private static String formatScore(String rawScore) {
        if (rawScore == null || rawScore.isEmpty() || !rawScore.contains(":")) {
            return "Score not found";
        }
        
        String[] parts = rawScore.split(":");

        if (parts.length != 2) {
            return "Score not found";
        }
        
        String left = parts[0];
        String right = parts[1];
        
        if (left.length() == 1) {
            left = "0" + left;
        }

        if (right.length() == 1) {
            right = "0" + right;
        }
        
        return left + ":" + right;
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
        
        return result;
    }
    
    private static void resetParameters() {
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        thresholdSlider.setValue(128);
        scaleSlider.setValue(100);
        
        brightness = 0;
        contrast = 1.0;
        thresholdValue = 128;
        scale = 1.0;
        
        if (sourceImage != null) {
            processImage();
        }
    }
    
    private static void refreshDisplay() {
        imagePanel.removeAll();
        imagePanel.setLayout(new BorderLayout());
        
        if (showInitialMessage && sourceImage == null) {
            JLabel messageLabel = new JLabel(
                "<html><div style='text-align: center;'>" +
                "Load screenshot to detect match time and score" +
                "</div></html>", 
                JLabel.CENTER
            );
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            messageLabel.setForeground(Color.BLACK);
            imagePanel.add(messageLabel, BorderLayout.CENTER);
        }
        else if (sourceImage != null) {
            JPanel imagesPanel = new JPanel(new GridLayout(1, 3, 10, 10));
            imagesPanel.setBackground(Color.WHITE);
            imagesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            imagesPanel.add(createImagePanel(sourceImage, "Original screenshot"));
            imagesPanel.add(createImagePanel(processedImage, "Processed image"));
            imagesPanel.add(createResultPanel());
            
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
            JLabel imageLabel = new JLabel(new ImageIcon(scaleImageForDisplay(image, 400)));
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            panel.add(imageLabel, BorderLayout.CENTER);
        }
        else {
            JLabel placeholder = new JLabel("Not available", JLabel.CENTER);
            placeholder.setForeground(Color.BLACK);
            placeholder.setFont(new Font("Arial", Font.PLAIN, 14));
            panel.add(placeholder, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    private static JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Detection result"));
        
        JPanel resultPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        resultPanel.setBackground(Color.WHITE);
        
        JLabel timeLabel = new JLabel("Time: " + detectedTime, JLabel.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        JLabel scoreLabel = new JLabel("Score: " + detectedScore, JLabel.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        if (detectedTime.equals("Time not found") || detectedTime.equals("Recognition error")) {
            timeLabel.setForeground(Color.RED);
        }
        else {
            timeLabel.setForeground(Color.GREEN);
        }
        
        if (detectedScore.equals("Score not found") || detectedScore.equals("Recognition error")) {
            scoreLabel.setForeground(Color.RED);
        }
        else {
            scoreLabel.setForeground(Color.GREEN);
        }
        
        resultPanel.add(timeLabel);
        resultPanel.add(scoreLabel);
        panel.add(resultPanel, BorderLayout.CENTER);

        return panel;
    }
    
    private static BufferedImage scaleImageForDisplay(BufferedImage original, int maxSize) {
        if (original == null) return null;
        
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
        sourceImage = null;
        processedImage = null;
        sourceImagePath = null;
        detectedTime = "Not detected";
        detectedScore = "Not detected";
        showInitialMessage = false;
        
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        thresholdSlider.setValue(128);
        scaleSlider.setValue(100);
        
        brightness = 0;
        contrast = 1.0;
        thresholdValue = 128;
        scale = 1.0;
        
        refreshDisplay();
    }
}
