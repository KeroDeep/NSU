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
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfByte;
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
    private static List<BufferedImage> originalGrayImages = new ArrayList<>();
    private static List<Mat> noisyImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();

    private static FileManager currentFileManager = null;
    
    private static int gaussianKernelSize = 5;
    private static double gaussianSigma = 1.5;
    private static int medianKernelSize = 5;
    private static double noiseStd = 25;
    private static int noiseRange = 50;
    private static boolean useGaussianNoise = true;
    
    private static final int MODE_NOISE_REMOVAL = 0;
    private static final int MODE_BORDER_SELECTION = 1;
    private static int currentMode = MODE_NOISE_REMOVAL;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Image filtering");
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        JButton openBtn = new JButton("Open file manager");
        JButton noiseRemovalBtn = new JButton("Noise removal");
        JButton borderSelectionBtn = new JButton("Border selection");
        JButton clearBtn = new JButton("Clear all");
        JButton noiseTypeBtn = new JButton("Noise: Gaussian");
        
        JSlider gaussianSizeSlider = new JSlider(3, 15, gaussianKernelSize);
        gaussianSizeSlider.setMajorTickSpacing(2);
        gaussianSizeSlider.setMinorTickSpacing(1);
        gaussianSizeSlider.setPaintTicks(true);
        gaussianSizeSlider.setPaintLabels(true);
        
        JSlider gaussianSigmaSlider = new JSlider(1, 50, (int)(gaussianSigma * 10));
        gaussianSigmaSlider.setMajorTickSpacing(10);
        gaussianSigmaSlider.setMinorTickSpacing(2);
        gaussianSigmaSlider.setPaintTicks(true);
        gaussianSigmaSlider.setPaintLabels(true);
        
        JSlider medianSizeSlider = new JSlider(3, 15, medianKernelSize);
        medianSizeSlider.setMajorTickSpacing(2);
        medianSizeSlider.setMinorTickSpacing(1);
        medianSizeSlider.setPaintTicks(true);
        medianSizeSlider.setPaintLabels(true);
        
        JSlider noiseStdSlider = new JSlider(0, 255, (int)noiseStd);
        noiseStdSlider.setMajorTickSpacing(50);
        noiseStdSlider.setMinorTickSpacing(10);
        noiseStdSlider.setPaintTicks(true);
        noiseStdSlider.setPaintLabels(true);
        
        JSlider noiseRangeSlider = new JSlider(0, 255, noiseRange);
        noiseRangeSlider.setMajorTickSpacing(50);
        noiseRangeSlider.setMinorTickSpacing(10);
        noiseRangeSlider.setPaintTicks(true);
        noiseRangeSlider.setPaintLabels(true);
        
        JPanel gaussianSizeLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gaussianSizeLabelPanel.setPreferredSize(new Dimension(150, 25));
        gaussianSizeLabelPanel.add(new JLabel("Gauss size:"));
        
        JPanel gaussianSigmaLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gaussianSigmaLabelPanel.setPreferredSize(new Dimension(150, 25));
        gaussianSigmaLabelPanel.add(new JLabel("Gauss sigma:"));
        
        JPanel medianSizeLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        medianSizeLabelPanel.setPreferredSize(new Dimension(150, 25));
        medianSizeLabelPanel.add(new JLabel("Median size:"));
        
        JPanel noiseLevelLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        noiseLevelLabelPanel.setPreferredSize(new Dimension(150, 25));
        JLabel noiseLevelLabel = new JLabel("Noise level (Gaussian):");
        noiseLevelLabelPanel.add(noiseLevelLabel);
        
        JPanel gaussianSizePanel = new JPanel(new BorderLayout());
        gaussianSizePanel.add(gaussianSizeLabelPanel, BorderLayout.WEST);
        gaussianSizePanel.add(gaussianSizeSlider, BorderLayout.CENTER);
        
        JPanel gaussianSigmaPanel = new JPanel(new BorderLayout());
        gaussianSigmaPanel.add(gaussianSigmaLabelPanel, BorderLayout.WEST);
        gaussianSigmaPanel.add(gaussianSigmaSlider, BorderLayout.CENTER);
        
        JPanel medianSizePanel = new JPanel(new BorderLayout());
        medianSizePanel.add(medianSizeLabelPanel, BorderLayout.WEST);
        medianSizePanel.add(medianSizeSlider, BorderLayout.CENTER);
        
        JPanel noiseLevelPanel = new JPanel(new BorderLayout());
        noiseLevelPanel.add(noiseLevelLabelPanel, BorderLayout.WEST);
        noiseLevelPanel.add(noiseStdSlider, BorderLayout.CENTER);
        
        gaussianSizeSlider.addChangeListener(event -> {
            gaussianKernelSize = gaussianSizeSlider.getValue();

            if (gaussianKernelSize % 2 == 0) {
                gaussianKernelSize++;
            }

            refreshDisplay();
        });
        
        gaussianSigmaSlider.addChangeListener(event -> {
            gaussianSigma = gaussianSigmaSlider.getValue() / 10.0;
            refreshDisplay();
        });
        
        medianSizeSlider.addChangeListener(event -> {
            medianKernelSize = medianSizeSlider.getValue();

            if (medianKernelSize % 2 == 0) {
                medianKernelSize++;
            }

            refreshDisplay();
        });
        
        noiseStdSlider.addChangeListener(event -> {
            noiseStd = noiseStdSlider.getValue();
            regenerateNoise();
            refreshDisplay();
        });
        
        noiseRangeSlider.addChangeListener(event -> {
            noiseRange = noiseRangeSlider.getValue();
            regenerateNoise();
            refreshDisplay();
        });
        
        openBtn.addActionListener(event -> openFileManager());
        noiseRemovalBtn.addActionListener(event -> {
            currentMode = MODE_NOISE_REMOVAL;
            refreshDisplay();
        });

        borderSelectionBtn.addActionListener(event -> {
            currentMode = MODE_BORDER_SELECTION;
            refreshDisplay();
        });

        clearBtn.addActionListener(event -> clearAll());
        
        noiseTypeBtn.addActionListener(event -> {
            useGaussianNoise = !useGaussianNoise;
            noiseTypeBtn.setText("Noise: " + (useGaussianNoise ? "Gaussian" : "Uniform"));
            noiseLevelLabel.setText("Noise level (" + (useGaussianNoise ? "Gaussian" : "Uniform") + "):");
            
            noiseLevelPanel.remove(noiseLevelPanel.getComponent(1));

            if (useGaussianNoise) {
                noiseLevelPanel.add(noiseStdSlider, BorderLayout.CENTER);
            }
            else {
                noiseLevelPanel.add(noiseRangeSlider, BorderLayout.CENTER);
            }
            
            noiseLevelPanel.revalidate();
            noiseLevelPanel.repaint();
            regenerateNoise();
            refreshDisplay();
        });
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(openBtn, gbc);
        
        gbc.gridx = 1;
        panel.add(noiseRemovalBtn, gbc);
        
        gbc.gridx = 2;
        panel.add(borderSelectionBtn, gbc);
        
        gbc.gridx = 3;
        panel.add(clearBtn, gbc);
        
        gbc.gridx = 4;
        panel.add(noiseTypeBtn, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 5;
        panel.add(gaussianSizePanel, gbc);
        
        gbc.gridy = 2;
        panel.add(gaussianSigmaPanel, gbc);
        
        gbc.gridy = 3;
        panel.add(medianSizePanel, gbc);
        
        gbc.gridy = 4;
        panel.add(noiseLevelPanel, gbc);
        
        return panel;
    }
    
    private static void regenerateNoise() {
        noisyImages.clear();

        for (int i = 0; i < originalGrayImages.size(); i++) {
            Mat grayMat = bufferedImageToMat(originalGrayImages.get(i));
            Mat noisy = useGaussianNoise ? addGaussianNoise(grayMat, noiseStd) : addUniformNoise(grayMat, noiseRange);
            noisyImages.add(noisy);
            grayMat.release();
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
        for (String path : paths) {
            try {
                BufferedImage colorImage = ImageIO.read(new File(path));

                if (colorImage != null) {
                    BufferedImage grayImage = convertToGrayscale(colorImage);
                    originalColorImages.add(colorImage);
                    originalGrayImages.add(grayImage);
                    imagePaths.add(path);
                    
                    Mat grayMat = bufferedImageToMat(grayImage);
                    Mat noisy = useGaussianNoise ? 
                        addGaussianNoise(grayMat, noiseStd) : 
                        addUniformNoise(grayMat, noiseRange);
                    noisyImages.add(noisy);
                    grayMat.release();
                }
            }
            catch (Exception event) {
                System.err.println("Error loading image: " + path);
                event.printStackTrace();
            }
        }
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
            imagePanel.revalidate();
            imagePanel.repaint();

            return;
        }
        
        if (currentMode == MODE_NOISE_REMOVAL) {
            displayNoiseRemovalResults();
        }
        else {
            displayBorderSelectionResults();
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static void displayNoiseRemovalResults() {
        for (int i = 0; i < originalColorImages.size(); i++) {
            JPanel rowPanel = new JPanel(new GridLayout(1, 6, 5, 5));
            rowPanel.setBackground(Color.WHITE);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            
            addImageToPanel(rowPanel, originalColorImages.get(i), "Color original");
            addImageToPanel(rowPanel, originalGrayImages.get(i), "Gray original");
            
            Mat noisy = noisyImages.get(i);
            addImageToPanel(rowPanel, matToBufferedImage(noisy), "Noisy image");
            
            Mat gaussian = applyGaussianFilter(noisy.clone(), gaussianKernelSize, gaussianSigma);
            addImageToPanel(rowPanel, matToBufferedImage(gaussian), "Gaussian filter");
            
            Mat median = applyMedianFilter(noisy.clone(), medianKernelSize);
            addImageToPanel(rowPanel, matToBufferedImage(median), "Median filter");
            
            Mat custom = applyCustomFilter(noisy.clone());
            addImageToPanel(rowPanel, matToBufferedImage(custom), "Custom filter");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            gaussian.release();
            median.release();
            custom.release();
        }
    }
    
    private static void displayBorderSelectionResults() {
        for (int i = 0; i < originalColorImages.size(); i++) {
            JPanel rowPanel = new JPanel(new GridLayout(1, 6, 5, 5));
            rowPanel.setBackground(Color.WHITE);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            
            addImageToPanel(rowPanel, originalColorImages.get(i), "Color original");
            addImageToPanel(rowPanel, originalGrayImages.get(i), "Gray original");
            
            Mat originalGrayMat = bufferedImageToMat(originalGrayImages.get(i));
            
            Mat laplacian = applyLaplacian(originalGrayMat.clone());
            addImageToPanel(rowPanel, matToBufferedImage(laplacian), "Laplacian");
            
            Mat sobelX = applySobelX(originalGrayMat.clone());
            addImageToPanel(rowPanel, matToBufferedImage(sobelX), "Sobel X");
            
            Mat sobelY = applySobelY(originalGrayMat.clone());
            addImageToPanel(rowPanel, matToBufferedImage(sobelY), "Sobel Y");
            
            Mat sobelXY = combineSobel(sobelX.clone(), sobelY.clone());
            addImageToPanel(rowPanel, matToBufferedImage(sobelXY), "Sobel X+Y");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            originalGrayMat.release();
            laplacian.release();
            sobelX.release();
            sobelY.release();
            sobelXY.release();
        }
    }
    
    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        JLabel imageLabel = new JLabel(new ImageIcon(scaleImage(image, 200, 150)));
        
        container.add(titleLabel, BorderLayout.NORTH);
        container.add(imageLabel, BorderLayout.CENTER);
        
        parent.add(container);
    }
    
    private static Mat applyGaussianFilter(Mat image, int kernelSize, double sigma) {
        Mat result = new Mat();
        Imgproc.GaussianBlur(image, result, new Size(kernelSize, kernelSize), sigma);

        return result;
    }
    
    private static Mat applyMedianFilter(Mat image, int kernelSize) {
        Mat result = new Mat();
        Imgproc.medianBlur(image, result, kernelSize);

        return result;
    }
    
    private static Mat applyCustomFilter(Mat image) {
        Mat result = new Mat();
        Mat kernel = new Mat(3, 3, CvType.CV_32F);
        float[] kernelData = {-1, -1, -1, -1, 9, -1, -1, -1, -1};
        kernel.put(0, 0, kernelData);
        Imgproc.filter2D(image, result, -1, kernel);
        kernel.release();

        return result;
    }
    
    private static Mat applySobelX(Mat image) {
        Mat result = new Mat();
        Imgproc.Sobel(image, result, CvType.CV_16S, 1, 0);
        Core.convertScaleAbs(result, result);

        return result;
    }
    
    private static Mat applySobelY(Mat image) {
        Mat result = new Mat();
        Imgproc.Sobel(image, result, CvType.CV_16S, 0, 1);
        Core.convertScaleAbs(result, result);

        return result;
    }
    
    private static Mat combineSobel(Mat sobelX, Mat sobelY) {
        Mat result = new Mat();
        Core.addWeighted(sobelX, 0.5, sobelY, 0.5, 0, result);

        return result;
    }
    
    private static Mat applyLaplacian(Mat image) {
        Mat result = new Mat();
        Imgproc.Laplacian(image, result, CvType.CV_16S);
        Core.convertScaleAbs(result, result);

        return result;
    }
    
    private static Mat addGaussianNoise(Mat image, double std) {
        Mat noise = new Mat(image.size(), image.type());
        Mat result = new Mat();
        Core.randn(noise, 0, std);
        Core.add(image, noise, result);
        noise.release();

        return result;
    }

    private static Mat addUniformNoise(Mat image, int range) {
        Mat noise = new Mat(image.size(), image.type());
        Mat result = new Mat();
        Core.randu(noise, -range/2, range/2);
        Core.add(image, noise, result);
        noise.release();

        return result;
    }
    
    private static Mat bufferedImageToMat(BufferedImage image) {
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
    
    private static void clearAll() {
        originalColorImages.clear();
        originalGrayImages.clear();
        noisyImages.clear();
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
