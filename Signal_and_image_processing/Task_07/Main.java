import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class Main {
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static List<BufferedImage> originalColorImages = new ArrayList<>();
    private static List<BufferedImage> originalGrayImages = new ArrayList<>();
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
        
        JSlider noiseStdSlider = new JSlider(0, 100, (int)noiseStd);
        noiseStdSlider.setMajorTickSpacing(20);
        noiseStdSlider.setMinorTickSpacing(5);
        noiseStdSlider.setPaintTicks(true);
        noiseStdSlider.setPaintLabels(true);
        
        JSlider noiseRangeSlider = new JSlider(0, 100, noiseRange);
        noiseRangeSlider.setMajorTickSpacing(20);
        noiseRangeSlider.setMinorTickSpacing(5);
        noiseRangeSlider.setPaintTicks(true);
        noiseRangeSlider.setPaintLabels(true);
        
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
            refreshDisplay();
        });
        
        noiseRangeSlider.addChangeListener(event -> {
            noiseRange = noiseRangeSlider.getValue();
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
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(new JLabel("Gauss size:"), gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 3;
        panel.add(gaussianSizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(new JLabel("Gauss sigma:"), gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 3;
        panel.add(gaussianSigmaSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(new JLabel("Median size:"), gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 3;
        panel.add(medianSizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(new JLabel("Noise level:"), gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 3;

        if (useGaussianNoise) {
            panel.add(noiseStdSlider, gbc);
        }
        else {
            panel.add(noiseRangeSlider, gbc);
        }
        
        return panel;
    }
    
    private static void openFileManager() {
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(paths -> {
            currentFileManager.close();
            currentFileManager = null;
            loadImages(paths);
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
                }
            }
            catch (Exception event) {
                System.err.println("Error loading image: " + path);
                event.printStackTrace();
            }
        }
    }
    
    private static BufferedImage convertToGrayscale(BufferedImage colorImage) {
        BufferedImage grayImage = new BufferedImage(
            colorImage.getWidth(), colorImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(colorImage, 0, 0, null);
        g.dispose();
        return grayImage;
    }
    
    private static void refreshDisplay() {
        imagePanel.removeAll();
        
        if (originalColorImages.isEmpty()) {
            addLabel("No images loaded. Click «Open file manager» to load images.");
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
            
            BufferedImage noisy = useGaussianNoise ? 
                addGaussianNoise(originalGrayImages.get(i), noiseStd) :
                addUniformNoise(originalGrayImages.get(i), noiseRange);
            addImageToPanel(rowPanel, noisy, "Noisy image");
            
            BufferedImage gaussian = applyGaussianFilter(noisy, gaussianKernelSize, gaussianSigma);
            addImageToPanel(rowPanel, gaussian, "Gaussian filter");
            
            BufferedImage median = applyMedianFilter(noisy, medianKernelSize);
            addImageToPanel(rowPanel, median, "Median filter");
            
            BufferedImage custom = applyCustomFilter(noisy);
            addImageToPanel(rowPanel, custom, "Custom filter");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
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
            
            BufferedImage laplacian = applyLaplacian(originalGrayImages.get(i));
            addImageToPanel(rowPanel, laplacian, "Laplacian");
            
            BufferedImage sobelX = applySobelX(originalGrayImages.get(i));
            addImageToPanel(rowPanel, sobelX, "Sobel X");
            
            BufferedImage sobelY = applySobelY(originalGrayImages.get(i));
            addImageToPanel(rowPanel, sobelY, "Sobel Y");
            
            BufferedImage sobelXY = combineSobel(sobelX, sobelY);
            addImageToPanel(rowPanel, sobelXY, "Sobel X+Y");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
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
    
    private static BufferedImage applyGaussianFilter(BufferedImage image, int kernelSize, double sigma) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        float[] kernel = createGaussianKernel(kernelSize, sigma);
        int radius = kernelSize / 2;
        
        for (int y = radius; y < height - radius; y++) {
            for (int x = radius; x < width - radius; x++) {
                float sum = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        float weight = kernel[(ky + radius) * kernelSize + (kx + radius)];
                        sum += pixel * weight;
                    }
                }

                int gray = (int) Math.max(0, Math.min(255, sum));
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static float[] createGaussianKernel(int size, double sigma) {
        float[] kernel = new float[size * size];
        float sum = 0;
        
        int radius = size / 2;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float value = (float) Math.exp(-(x*x + y*y) / (2 * sigma * sigma));
                kernel[(y + radius) * size + (x + radius)] = value;
                sum += value;
            }
        }
        
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        
        return kernel;
    }
    
    private static BufferedImage applyMedianFilter(BufferedImage image, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int radius = kernelSize / 2;
        int[] window = new int[kernelSize * kernelSize];
        
        for (int y = radius; y < height - radius; y++) {
            for (int x = radius; x < width - radius; x++) {
                int index = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        window[index++] = image.getRGB(x + kx, y + ky) & 0xFF;
                    }
                }
                
                java.util.Arrays.sort(window);
                int median = window[window.length / 2];
                int rgb = (median << 16) | (median << 8) | median;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage applyCustomFilter(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        float[] kernel = {
            -1, -1, -1,
            -1,  9, -1,
            -1, -1, -1
        };
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float sum = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        float weight = kernel[(ky + 1) * 3 + (kx + 1)];
                        sum += pixel * weight;
                    }
                }

                int gray = (int) Math.max(0, Math.min(255, sum));
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage applySobelX(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int[] kernelX = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sum = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        int weight = kernelX[(ky + 1) * 3 + (kx + 1)];
                        sum += pixel * weight;
                    }
                }

                int gray = Math.max(0, Math.min(255, Math.abs(sum) / 4));
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage applySobelY(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int[] kernelY = {-1, -2, -1, 0, 0, 0, 1, 2, 1};
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sum = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        int weight = kernelY[(ky + 1) * 3 + (kx + 1)];
                        sum += pixel * weight;
                    }
                }

                int gray = Math.max(0, Math.min(255, Math.abs(sum) / 4));
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage combineSobel(BufferedImage sobelX, BufferedImage sobelY) {
        int width = sobelX.getWidth();
        int height = sobelX.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gx = sobelX.getRGB(x, y) & 0xFF;
                int gy = sobelY.getRGB(x, y) & 0xFF;
                int magnitude = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));
                int rgb = (magnitude << 16) | (magnitude << 8) | magnitude;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage applyLaplacian(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int[] kernel = {0, -1, 0, -1, 4, -1, 0, -1, 0};
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sum = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        int weight = kernel[(ky + 1) * 3 + (kx + 1)];
                        sum += pixel * weight;
                    }
                }

                int gray = Math.max(0, Math.min(255, Math.abs(sum)));
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage addGaussianNoise(BufferedImage image, double std) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        java.util.Random rand = new java.util.Random();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y) & 0xFF;
                double noise = rand.nextGaussian() * std;
                int newPixel = (int) Math.max(0, Math.min(255, pixel + noise));
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }
    
    private static BufferedImage addUniformNoise(BufferedImage image, int range) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        java.util.Random rand = new java.util.Random();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y) & 0xFF;
                int noise = rand.nextInt(2 * range + 1) - range;
                int newPixel = Math.max(0, Math.min(255, pixel + noise));
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
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
