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
    private static List<BufferedImage> originalImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Image Filtering Demo - Task 7");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(1200, 800);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = new JPanel(new FlowLayout());
                JButton openBtn = new JButton("Open File Manager");
                JButton processBtn = new JButton("Process Images");
                JButton clearBtn = new JButton("Clear All");
                JButton addNoiseBtn = new JButton("Add Noise");
                
                imagePanel = new JPanel();
                imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
                JScrollPane scrollPane = new JScrollPane(imagePanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                FileManager fileManager = new FileManager();
                
                fileManager.setOnImagesSelected(paths -> {
                    fileManager.close();
                    loadImages(paths);
                    displayOriginalImages();
                    mainFrame.setTitle("Image Filtering - Loaded: " + originalImages.size() + " images");
                });
                
                openBtn.addActionListener(e -> fileManager.showFileManager());
                
                processBtn.addActionListener(e -> {
                    if (originalImages.isEmpty()) {
                        JOptionPane.showMessageDialog(mainFrame, "Please load images first!");
                        return;
                    }
                    processAllFilters();
                });
                
                addNoiseBtn.addActionListener(e -> {
                    if (originalImages.isEmpty()) {
                        JOptionPane.showMessageDialog(mainFrame, "Please load images first!");
                        return;
                    }
                    addNoiseAndProcess();
                });
                
                clearBtn.addActionListener(e -> {
                    originalImages.clear();
                    imagePaths.clear();
                    imagePanel.removeAll();
                    imagePanel.revalidate();
                    imagePanel.repaint();
                    mainFrame.setTitle("Image Filtering Demo - Task 7");
                });
                
                controlPanel.add(openBtn);
                controlPanel.add(processBtn);
                controlPanel.add(addNoiseBtn);
                controlPanel.add(clearBtn);
                
                mainFrame.add(controlPanel, BorderLayout.NORTH);
                mainFrame.add(scrollPane, BorderLayout.CENTER);
                
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private static void loadImages(String[] paths) {
        originalImages.clear();
        imagePaths.clear();
        
        for (String path : paths) {
            try {
                BufferedImage image = ImageIO.read(new File(path));
                if (image != null) {
                    BufferedImage grayImage = convertToGrayscale(image);
                    originalImages.add(grayImage);
                    imagePaths.add(path);
                }
            } catch (Exception e) {
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
    
    private static void displayOriginalImages() {
        imagePanel.removeAll();
        
        if (originalImages.isEmpty()) {
            addLabel("No images loaded");
            return;
        }
        
        for (int i = 0; i < originalImages.size(); i++) {
            BufferedImage image = originalImages.get(i);
            String fileName = new File(imagePaths.get(i)).getName();
            
            addLabel("Original: " + fileName);
            displayImage(image, "Original");
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static void processAllFilters() {
        imagePanel.removeAll();
        
        for (int i = 0; i < originalImages.size(); i++) {
            BufferedImage original = originalImages.get(i);
            String fileName = new File(imagePaths.get(i)).getName();
            
            addLabel("=== " + fileName + " ===");
            
            addLabel("Original");
            displayImage(original, "Original");
            
            BufferedImage gaussian = applyGaussianFilter(original, 5);
            addLabel("Gaussian Filter");
            displayImage(gaussian, "Gaussian");
            
            BufferedImage median = applyMedianFilter(original, 5);
            addLabel("Median Filter");
            displayImage(median, "Median");
            
            BufferedImage custom = applyCustomFilter(original);
            addLabel("Custom Filter");
            displayImage(custom, "Custom");
            
            BufferedImage sobelX = applySobelX(original);
            addLabel("Sobel X");
            displayImage(sobelX, "SobelX");
            
            BufferedImage sobelY = applySobelY(original);
            addLabel("Sobel Y");
            displayImage(sobelY, "SobelY");
            
            BufferedImage sobelXY = combineSobel(sobelX, sobelY);
            addLabel("Sobel X+Y");
            displayImage(sobelXY, "SobelXY");
            
            BufferedImage laplacian = applyLaplacian(original);
            addLabel("Laplacian");
            displayImage(laplacian, "Laplacian");
            
            BufferedImage canny = applyCanny(original);
            addLabel("Canny");
            displayImage(canny, "Canny");
            
            imagePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static void addNoiseAndProcess() {
        imagePanel.removeAll();
        
        for (int i = 0; i < originalImages.size(); i++) {
            BufferedImage original = originalImages.get(i);
            String fileName = new File(imagePaths.get(i)).getName();
            
            addLabel("=== Noise: " + fileName + " ===");
            
            addLabel("Original");
            displayImage(original, "Original");
            
            BufferedImage gaussianNoise = addGaussianNoise(original, 25);
            addLabel("Gaussian Noise");
            displayImage(gaussianNoise, "GaussianNoise");
            
            BufferedImage uniformNoise = addUniformNoise(original, 50);
            addLabel("Uniform Noise");
            displayImage(uniformNoise, "UniformNoise");
            
            BufferedImage denoisedGaussian = applyGaussianFilter(gaussianNoise, 5);
            addLabel("Gaussian Denoise");
            displayImage(denoisedGaussian, "DenoiseGaussian");
            
            BufferedImage denoisedGaussianMedian = applyMedianFilter(gaussianNoise, 5);
            addLabel("Median Denoise Gaussian");
            displayImage(denoisedGaussianMedian, "DenoiseGaussianMedian");
            
            BufferedImage denoisedUniform = applyGaussianFilter(uniformNoise, 5);
            addLabel("Gaussian Denoise Uniform");
            displayImage(denoisedUniform, "DenoiseUniform");
            
            BufferedImage denoisedUniformMedian = applyMedianFilter(uniformNoise, 5);
            addLabel("Median Denoise Uniform");
            displayImage(denoisedUniformMedian, "DenoiseUniformMedian");
            
            imagePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static BufferedImage applyGaussianFilter(BufferedImage image, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        float[] kernel = createGaussianKernel(kernelSize);
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
    
    private static float[] createGaussianKernel(int size) {
        float[] kernel = new float[size * size];
        float sigma = size / 3.0f;
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
    
    private static BufferedImage applyCanny(BufferedImage image) {
        BufferedImage sobelX = applySobelX(image);
        BufferedImage sobelY = applySobelY(image);
        BufferedImage gradient = combineSobel(sobelX, sobelY);
        
        int width = gradient.getWidth();
        int height = gradient.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int lowThreshold = 50;
        int highThreshold = 150;
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int magnitude = gradient.getRGB(x, y) & 0xFF;
                int gray = (magnitude > highThreshold) ? 255 : (magnitude > lowThreshold ? 128 : 0);
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
    
    private static void displayImage(BufferedImage image, String title) {
        try {
            JLabel imageLabel = new JLabel(new ImageIcon(scaleImage(image, 300, 200)));
            JPanel imageContainer = new JPanel(new BorderLayout());
            imageContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            imageContainer.add(imageLabel, BorderLayout.CENTER);
            imagePanel.add(imageContainer);
        } catch (Exception e) {
        }
    }
    
    private static void addLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        imagePanel.add(label);
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
