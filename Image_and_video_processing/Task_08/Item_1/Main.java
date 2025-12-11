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
    private static int cannyThreshold1 = 50;
    private static int cannyThreshold2 = 150;
    private static int cannyApertureSize = 3;
    private static boolean useL2Gradient = false;
    private static boolean applyBlur = true;
    private static int blurSize = 3;
    private static double blurSigma = 1.0;

    private static JSlider threshold1Slider;
    private static JSlider threshold2Slider;
    private static JRadioButton l1Button;
    private static JRadioButton l2Button;
    private static JComboBox<String> apertureComboBox;
    private static JCheckBox blurCheckBox;
    private static JSlider blurSizeSlider;
    private static JSlider blurSigmaSlider;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Objects contours selecting");
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
        l1Button = new JRadioButton("L1 gradient", !useL2Gradient);
        l2Button = new JRadioButton("L2 gradient", useL2Gradient);
        blurCheckBox = new JCheckBox("Apply blur", applyBlur);
        
        ButtonGroup gradientGroup = new ButtonGroup();
        gradientGroup.add(l1Button);
        gradientGroup.add(l2Button);

        threshold1Slider = new JSlider(0, 255, cannyThreshold1);
        threshold1Slider.setMajorTickSpacing(50);
        threshold1Slider.setMinorTickSpacing(10);
        threshold1Slider.setPaintTicks(true);
        threshold1Slider.setPaintLabels(true);

        threshold2Slider = new JSlider(0, 255, cannyThreshold2);
        threshold2Slider.setMajorTickSpacing(50);
        threshold2Slider.setMinorTickSpacing(10);
        threshold2Slider.setPaintTicks(true);
        threshold2Slider.setPaintLabels(true);

        apertureComboBox = new JComboBox<>(new String[]{"3x3", "5x5", "7x7"});
        apertureComboBox.setSelectedIndex(0);

        blurSizeSlider = new JSlider(1, 15, blurSize);
        blurSizeSlider.setMajorTickSpacing(2);
        blurSizeSlider.setMinorTickSpacing(1);
        blurSizeSlider.setPaintTicks(true);
        blurSizeSlider.setPaintLabels(true);
        blurSizeSlider.setEnabled(applyBlur);

        blurSigmaSlider = new JSlider(0, 100, (int)(blurSigma * 10));
        blurSigmaSlider.setMajorTickSpacing(20);
        blurSigmaSlider.setMinorTickSpacing(5);
        blurSigmaSlider.setPaintTicks(true);
        blurSigmaSlider.setPaintLabels(true);
        blurSigmaSlider.setEnabled(applyBlur);

        threshold1Slider.addChangeListener(event -> {
            cannyThreshold1 = threshold1Slider.getValue();

            if (cannyThreshold1 >= cannyThreshold2) {
                cannyThreshold2 = Math.min(255, cannyThreshold1 + 1);
                threshold2Slider.setValue(cannyThreshold2);
            }

            refreshDisplay();
        });

        threshold2Slider.addChangeListener(event -> {
            cannyThreshold2 = threshold2Slider.getValue();

            if (cannyThreshold2 <= cannyThreshold1) {
                cannyThreshold1 = Math.max(0, cannyThreshold2 - 1);
                threshold1Slider.setValue(cannyThreshold1);
            }

            refreshDisplay();
        });

        apertureComboBox.addActionListener(event -> {
            String selected = (String) apertureComboBox.getSelectedItem();
            cannyApertureSize = Integer.parseInt(selected.substring(0, 1));
            refreshDisplay();
        });

        l1Button.addActionListener(event -> {
            useL2Gradient = false;
            refreshDisplay();
        });

        l2Button.addActionListener(event -> {
            useL2Gradient = true;
            refreshDisplay();
        });

        blurCheckBox.addActionListener(event -> {
            applyBlur = blurCheckBox.isSelected();
            blurSizeSlider.setEnabled(applyBlur);
            blurSigmaSlider.setEnabled(applyBlur);
            refreshDisplay();
        });

        blurSizeSlider.addChangeListener(event -> {
            blurSize = blurSizeSlider.getValue();
            
            if (blurSize % 2 == 0) {
                blurSize++;
            }

            refreshDisplay();
        });

        blurSigmaSlider.addChangeListener(event -> {
            blurSigma = blurSigmaSlider.getValue() / 10.0;
            refreshDisplay();
        });

        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        resetBtn.addActionListener(event -> resetParameters());

        buttonPanel.add(openBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(resetBtn);
        buttonPanel.add(blurCheckBox);

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

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Gradient:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 1;
        controlPanel.add(l1Button, gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 1;
        controlPanel.add(l2Button, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Aperture:"), gbc);
        
        gbc.gridx = 4; gbc.gridwidth = 1;
        controlPanel.add(apertureComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Blur size:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        controlPanel.add(blurSizeSlider, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Blur sigma:"), gbc);
        
        gbc.gridx = 4; gbc.gridwidth = 1;
        controlPanel.add(blurSigmaSlider, gbc);

        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(controlPanel);

        return mainPanel;
    }

    private static void resetParameters() {
        cannyThreshold1 = 50;
        cannyThreshold2 = 150;
        cannyApertureSize = 3;
        useL2Gradient = false;
        applyBlur = true;
        blurSize = 3;
        blurSigma = 1.0;
        
        threshold1Slider.setValue(cannyThreshold1);
        threshold2Slider.setValue(cannyThreshold2);
        l1Button.setSelected(!useL2Gradient);
        l2Button.setSelected(useL2Gradient);
        apertureComboBox.setSelectedIndex(0);
        blurCheckBox.setSelected(applyBlur);
        blurSizeSlider.setValue(blurSize);
        blurSigmaSlider.setValue((int)(blurSigma * 10));
        blurSizeSlider.setEnabled(applyBlur);
        blurSigmaSlider.setEnabled(applyBlur);
        
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
            
            Mat grayMat = bufferedImageToGrayMat(grayOriginal);
            Mat processedGray = grayMat.clone();
            
            if (applyBlur) {
                Imgproc.GaussianBlur(processedGray, processedGray, new Size(blurSize, blurSize), blurSigma);
            }
            
            Mat edges = new Mat();
            Mat colorMat = bufferedImageToMat(colorOriginal);
            
            Imgproc.Canny(processedGray, edges, cannyThreshold1, cannyThreshold2, cannyApertureSize, useL2Gradient);
            
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            
            Mat contoursOnBlack = Mat.zeros(edges.size(), CvType.CV_8UC3);
            Mat contourOverlay = colorMat.clone();
            
            Scalar white = new Scalar(255, 255, 255);
            Scalar red = new Scalar(0, 0, 255);
            
            Imgproc.drawContours(contoursOnBlack, contours, -1, white, 2);
            Imgproc.drawContours(contourOverlay, contours, -1, red, 2);
            
            BufferedImage processedGrayImage = matToBufferedImage(processedGray);
            BufferedImage contoursImage = matToBufferedImage(contoursOnBlack);
            BufferedImage overlayImage = matToBufferedImage(contourOverlay);
            
            addImageToPanel(rowPanel, colorOriginal, "Color original");
            addImageToPanel(rowPanel, processedGrayImage, "Processed gray");
            addImageToPanel(rowPanel, contoursImage, "Detected contours (" + contours.size() + ")");
            addImageToPanel(rowPanel, overlayImage, "Contours overlay");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            grayMat.release();
            processedGray.release();
            edges.release();
            colorMat.release();
            hierarchy.release();
            contoursOnBlack.release();
            contourOverlay.release();
        }
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
