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
    private static int cannyThreshold1 = 50;
    private static int cannyThreshold2 = 150;
    private static int cannyApertureSize = 3;
    private static boolean useL1Gradient = true;
    private static int contourMode = 0;
    private static int contourMethod = 1;

    private static JSlider threshold1Slider;
    private static JSlider threshold2Slider;
    private static JRadioButton l1Button;
    private static JRadioButton l2Button;
    private static JComboBox<String> apertureComboBox;
    private static JComboBox<String> contourModeComboBox;
    private static JComboBox<String> contourMethodComboBox;

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
        l1Button = new JRadioButton("L1 gradient", useL1Gradient);
        l2Button = new JRadioButton("L2 gradient", !useL1Gradient);
        
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

        apertureComboBox = new JComboBox<>(new String[]{"3x3", "5x5", "7x7", "9x9"});
        apertureComboBox.setSelectedIndex(0);

        contourModeComboBox = new JComboBox<>(new String[]{"External contours", "All contours", "Two-level", "Full hierarchy"});
        contourModeComboBox.setSelectedIndex(0);

        contourMethodComboBox = new JComboBox<>(new String[]{"Store all points", "Compress lines"});
        contourMethodComboBox.setSelectedIndex(0);

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
            useL1Gradient = true;
            refreshDisplay();
        });

        l2Button.addActionListener(event -> {
            useL1Gradient = false;
            refreshDisplay();
        });

        contourModeComboBox.addActionListener(event -> {
            contourMode = contourModeComboBox.getSelectedIndex();
            refreshDisplay();
        });

        contourMethodComboBox.addActionListener(event -> {
            contourMethod = contourMethodComboBox.getSelectedIndex() + 1;
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Lower threshold:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 4;
        controlPanel.add(threshold1Slider, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Upper threshold:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 4;
        controlPanel.add(threshold2Slider, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Gradient:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        controlPanel.add(l1Button, gbc);
        
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        controlPanel.add(l2Button, gbc);
        
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Aperture:"), gbc);
        
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        controlPanel.add(apertureComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Contour mode:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        controlPanel.add(contourModeComboBox, gbc);
        
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Approximation method:"), gbc);
        
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        controlPanel.add(contourMethodComboBox, gbc);

        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(controlPanel);

        return mainPanel;
    }

    private static void resetParameters() {
        cannyThreshold1 = 50;
        cannyThreshold2 = 150;
        cannyApertureSize = 3;
        useL1Gradient = true;
        contourMode = 0;
        contourMethod = 1;
        
        threshold1Slider.setValue(cannyThreshold1);
        threshold2Slider.setValue(cannyThreshold2);
        l1Button.setSelected(useL1Gradient);
        l2Button.setSelected(!useL1Gradient);
        apertureComboBox.setSelectedIndex(0);
        contourModeComboBox.setSelectedIndex(0);
        contourMethodComboBox.setSelectedIndex(0);
        
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
            addLabel("No images loaded. Click «Open file manager» to load images.");
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
            BufferedImage cannyEdges = applyCanny(grayOriginal);
            List<List<Point>> contours = findContours(cannyEdges);
            BufferedImage contoursImage = drawContoursOnBlack(contours, colorOriginal.getWidth(), colorOriginal.getHeight());
            BufferedImage contourOverlay = createContourOverlay(colorOriginal, contours, Color.RED);
            
            addImageToPanel(rowPanel, colorOriginal, "Color original");
            addImageToPanel(rowPanel, grayOriginal, "Gray original");
            addImageToPanel(rowPanel, contoursImage, "Detected contours");
            addImageToPanel(rowPanel, contourOverlay, "Contours overlay");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
    }

    private static BufferedImage applyCanny(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage blurred = applyGaussianBlur(image, cannyApertureSize, 1.4f);
        BufferedImage sobelX = applySobelX(blurred);
        BufferedImage sobelY = applySobelY(blurred);
        
        float[][] gradientMagnitude = new float[height][width];
        float[][] gradientDirection = new float[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gx = sobelX.getRGB(x, y) & 0xFF;
                int gy = sobelY.getRGB(x, y) & 0xFF;
                float fx = (gx - 128) * 2.0f;
                float fy = (gy - 128) * 2.0f;
                
                if (useL1Gradient) {
                    gradientMagnitude[y][x] = Math.abs(fx) + Math.abs(fy);
                }
                else {
                    gradientMagnitude[y][x] = (float) Math.sqrt(fx * fx + fy * fy);
                }

                gradientDirection[y][x] = (float) Math.toDegrees(Math.atan2(fy, fx));

                if (gradientDirection[y][x] < 0) {
                    gradientDirection[y][x] += 180;
                }
            }
        }
        
        BufferedImage suppressed = nonMaximumSuppression(gradientMagnitude, gradientDirection, width, height);
        boolean[][] strongEdges = new boolean[height][width];
        boolean[][] weakEdges = new boolean[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = suppressed.getRGB(x, y) & 0xFF;

                if (pixel >= cannyThreshold2) {
                    strongEdges[y][x] = true;
                }
                else if (pixel >= cannyThreshold1) {
                    weakEdges[y][x] = true;
                }
            }
        }
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (weakEdges[y][x]) {
                    if (strongEdges[y-1][x-1] || strongEdges[y-1][x] || strongEdges[y-1][x+1] || strongEdges[y][x-1] || strongEdges[y][x+1] || strongEdges[y+1][x-1] || strongEdges[y+1][x] || strongEdges[y+1][x+1]) {
                        strongEdges[y][x] = true;
                    }
                }
            }
        }
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newPixel = strongEdges[y][x] ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }
        
        return result;
    }

    private static BufferedImage applyGaussianBlur(BufferedImage image, int kernelSize, float sigma) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        float[] kernel = createGaussianKernel(kernelSize, sigma);
        int radius = kernelSize / 2;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int px = Math.min(Math.max(x + kx, 0), width - 1);
                        int py = Math.min(Math.max(y + ky, 0), height - 1);
                        int pixel = image.getRGB(px, py) & 0xFF;
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

    private static float[] createGaussianKernel(int size, float sigma) {
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

    private static BufferedImage nonMaximumSuppression(float[][] magnitude, float[][] direction, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float angle = direction[y][x];
                float mag = magnitude[y][x];
                float mag1 = 0, mag2 = 0;
                
                if ((angle >= 0 && angle < 22.5) || (angle >= 157.5 && angle <= 180)) {
                    mag1 = magnitude[y][x-1];
                    mag2 = magnitude[y][x+1];
                }
                else if (angle >= 22.5 && angle < 67.5) {
                    mag1 = magnitude[y-1][x+1];
                    mag2 = magnitude[y+1][x-1];
                }
                else if (angle >= 67.5 && angle < 112.5) {
                    mag1 = magnitude[y-1][x];
                    mag2 = magnitude[y+1][x];
                }
                else if (angle >= 112.5 && angle < 157.5) {
                    mag1 = magnitude[y-1][x-1];
                    mag2 = magnitude[y+1][x+1];
                }
                
                int newPixel = (mag >= mag1 && mag >= mag2) ? (int) mag : 0;
                newPixel = Math.min(255, newPixel);
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private static List<List<Point>> findContours(BufferedImage binaryImage) {
        List<List<Point>> contours = new ArrayList<>();
        int width = binaryImage.getWidth();
        int height = binaryImage.getHeight();
        boolean[][] visited = new boolean[height][width];
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, -1, -1, -1, 0, 1, 1, 1};
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = binaryImage.getRGB(x, y) & 0xFF;

                if (pixel > 128 && !visited[y][x]) {
                    List<Point> contour = new ArrayList<>();
                    traceContourMoore(binaryImage, x, y, visited, contour, width, height, dx, dy);

                    if (contour.size() > 5) {
                        contours.add(contour);
                    }
                }
            }
        }

        return contours;
    }

    private static void traceContourMoore(BufferedImage image, int startX, int startY, boolean[][] visited, List<Point> contour, int width, int height, int[] dx, int[] dy) {
        int x = startX;
        int y = startY;
        int direction = 0;
        int maxContourSize = 10000;
        
        do {
            if (contour.size() >= maxContourSize) {
                break;
            }

            contour.add(new Point(x, y));
            visited[y][x] = true;
            int startDir = (direction + 6) % 8;
            boolean found = false;
            int newDirection = direction;
            
            for (int i = 0; i < 8; i++) {
                int testDir = (startDir + i) % 8;
                int nx = x + dx[testDir];
                int ny = y + dy[testDir];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int pixel = image.getRGB(nx, ny) & 0xFF;

                    if (pixel > 128) {
                        x = nx;
                        y = ny;
                        newDirection = testDir;
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                break;
            }

            direction = newDirection;
        } while ((x != startX || y != startY) || contour.size() == 1);
        
        if (contour.size() > 1 && !contour.get(0).equals(contour.get(contour.size() - 1))) {
            contour.add(new Point(contour.get(0).x, contour.get(0).y));
        }
    }

    private static BufferedImage drawContoursOnBlack(List<List<Point>> contours, int imageWidth, int imageHeight) {
        BufferedImage result = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));

        for (List<Point> contour : contours) {
            if (contour.size() > 1) {
                Point prev = contour.get(0);

                for (int i = 1; i < contour.size(); i++) {
                    Point current = contour.get(i);
                    g2d.drawLine(prev.x, prev.y, current.x, current.y);
                    prev = current;
                }
            }
        }

        g2d.dispose();

        return result;
    }

    private static BufferedImage createContourOverlay(BufferedImage background, List<List<Point>> contours, Color contourColor) {
        BufferedImage result = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(background, 0, 0, null);
        g2d.setColor(contourColor);
        g2d.setStroke(new BasicStroke(1.5f));

        for (List<Point> contour : contours) {
            if (contour.size() > 1) {
                Point prev = contour.get(0);

                for (int i = 1; i < contour.size(); i++) {
                    Point current = contour.get(i);
                    g2d.drawLine(prev.x, prev.y, current.x, current.y);
                    prev = current;
                }
            }
        }

        g2d.dispose();

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

                int gray = Math.max(0, Math.min(255, (sum / 4) + 128));
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

                int gray = Math.max(0, Math.min(255, (sum / 4) + 128));
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
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
