import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.Rectangle;

public class Main {
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static List<BufferedImage> originalColorImages = new ArrayList<>();
    private static List<BufferedImage> originalGrayImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();
    private static FileManager currentFileManager = null;
    private static JButton openBtn;
    private static JButton clearBtn;

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
        openBtn = new JButton("Open file manager");
        clearBtn = new JButton("Clear all");
        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        buttonPanel.add(openBtn);
        buttonPanel.add(clearBtn);
        mainPanel.add(buttonPanel);

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
            Object[] result = analyzeImageEnhanced(colorOriginal, grayOriginal);
            
            addImageToPanel(rowPanel, colorOriginal, "Original");
            addImageToPanel(rowPanel, grayOriginal, "Grayscale");
            addImageToPanel(rowPanel, (BufferedImage) result[1], "Detected objects");
            
            String overlayTitle = String.format("Overlay (%.1f%%)", (Double) result[3]);
            addImageToPanel(rowPanel, (BufferedImage) result[2], overlayTitle);
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
    }

    private static Object[] analyzeImageEnhanced(BufferedImage colorOriginal, BufferedImage grayOriginal) {
        int width = colorOriginal.getWidth();
        int height = colorOriginal.getHeight();
        List<Object[]> bestObjects = new ArrayList<>();
        double bestScore = 0;

        for (int method = 0; method < 6; method++) {
            BufferedImage binary = applySegmentationMethod(grayOriginal, method);
            List<Object[]> objects = findObjectsWithMorphology(binary);
            List<Object[]> filteredObjects = filterObjectsByQuality(objects, width, height);
            double score = evaluateSegmentationQuality(filteredObjects, width, height);

            if (score > bestScore && !filteredObjects.isEmpty()) {
                bestScore = score;
                bestObjects = filteredObjects;
            }
        }

        if (bestObjects.isEmpty()) {
            BufferedImage binary = applySimpleThreshold(grayOriginal);
            bestObjects = findObjectsWithMorphology(binary);
        }

        String sizeInfo;
        BufferedImage objectsImage;
        BufferedImage overlayImage;
        double coveragePercentage = 0;

        if (bestObjects.isEmpty()) {
            sizeInfo = "No objects detected";
            objectsImage = createBlankImage(width, height);
            overlayImage = copyImage(colorOriginal);
        }
        else {
            int totalArea = 0;

            for (Object[] obj : bestObjects) {
                totalArea += (int) obj[1];
            }

            coveragePercentage = (double) totalArea / (width * height) * 100;
            sizeInfo = String.format("%.1f%%", coveragePercentage);
            objectsImage = visualizeObjects(bestObjects, width, height);
            overlayImage = createOverlay(colorOriginal, bestObjects);
        }

        return new Object[]{sizeInfo, objectsImage, overlayImage, coveragePercentage};
    }

    private static BufferedImage applySimpleThreshold(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        long sum = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sum += grayImage.getRGB(x, y) & 0xFF;
            }
        }

        int threshold = (int) (sum / (width * height));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayImage.getRGB(x, y) & 0xFF;
                int newPixel = (pixel < threshold) ? 255 : 0;

                if (!isBackgroundWhite(grayImage)) {
                    newPixel = (pixel > threshold) ? 255 : 0;
                }

                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return applyAdvancedMorphology(result);
    }

    private static BufferedImage visualizeObjects(List<Object[]> objects, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);

        for (Object[] obj : objects) {
            @SuppressWarnings("unchecked")
            List<int[]> pixels = (List<int[]>) obj[0];
            for (int[] p : pixels) {
                image.setRGB(p[0], p[1], Color.WHITE.getRGB());
            }
        }

        g2d.dispose();

        return image;
    }

    private static BufferedImage createOverlay(BufferedImage original, List<Object[]> objects) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage overlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = overlay.createGraphics();
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(new Color(255, 0, 0, 150));

        for (Object[] obj : objects) {
            @SuppressWarnings("unchecked")
            List<int[]> pixels = (List<int[]>) obj[0];
            for (int[] p : pixels) {
                overlay.setRGB(p[0], p[1], new Color(255, 0, 0, 150).getRGB());
            }
        }

        g2d.dispose();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        g2d = result.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.drawImage(overlay, 0, 0, null);
        g2d.dispose();

        return result;
    }

    private static BufferedImage applySegmentationMethod(BufferedImage grayImage, int method) {
        switch (method) {
            case 0: return applyOtsuWithEdgeRefinement(grayImage);
            case 1: return applyAdaptiveThresholdEnhanced(grayImage);
            case 2: return applyMultiScaleSegmentation(grayImage);
            case 3: return applyContrastEnhancementSegmentation(grayImage);
            case 4: return applyKMeansSegmentation(grayImage, 2);
            case 5: return applySimpleThreshold(grayImage);
            default: return applyOtsuWithEdgeRefinement(grayImage);
        }
    }

    private static BufferedImage applyKMeansSegmentation(BufferedImage grayImage, int k) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        int[] pixels = new int[width * height];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[index++] = grayImage.getRGB(x, y) & 0xFF;
            }
        }

        int[] centroids = new int[k];

        for (int i = 0; i < k; i++) {
            centroids[i] = (int) (Math.random() * 256);
        }

        int[] labels = new int[pixels.length];
        boolean changed = true;

        while (changed) {
            changed = false;
            int[] sums = new int[k];
            int[] counts = new int[k];

            for (int i = 0; i < pixels.length; i++) {
                int minDist = Integer.MAX_VALUE;
                int label = 0;

                for (int j = 0; j < k; j++) {
                    int dist = Math.abs(pixels[i] - centroids[j]);
                    
                    if (dist < minDist) {
                        minDist = dist;
                        label = j;
                    }
                }

                if (label != labels[i]) {
                    changed = true;
                    labels[i] = label;
                }

                sums[label] += pixels[i];
                counts[label]++;
            }

            for (int j = 0; j < k; j++) {
                if (counts[j] > 0) {
                    centroids[j] = sums[j] / counts[j];
                }
            }
        }

        if (centroids[0] > centroids[1]) {
            int temp = centroids[0];
            centroids[0] = centroids[1];
            centroids[1] = temp;
        }

        boolean backgroundIsWhite = isBackgroundWhite(grayImage);
        int objectLabel = backgroundIsWhite ? 0 : 1;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newPixel = (labels[index] == objectLabel) ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
                index++;
            }
        }

        return applyAdvancedMorphology(result);
    }

    private static BufferedImage applyOtsuWithEdgeRefinement(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage otsuBinary = applyOtsuThreshold(grayImage);
        BufferedImage edges = detectCannyEdges(grayImage);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int otsuPixel = otsuBinary.getRGB(x, y) & 0xFF;
                int edgePixel = edges.getRGB(x, y) & 0xFF;
                int newPixel = (edgePixel > 128 || otsuPixel > 128) ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return applyAdvancedMorphology(result);
    }

    private static BufferedImage applyOtsuThreshold(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int[] histogram = new int[256];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayImage.getRGB(x, y) & 0xFF;
                histogram[pixel]++;
            }
        }

        int total = width * height;
        float sum = 0;

        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        float sumB = 0;
        int wB = 0;
        int wF = 0;
        float varMax = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            wB += histogram[i];

            if (wB == 0) {
                continue;
            }

            wF = total - wB;

            if (wF == 0) {
                break;
            }

            sumB += i * histogram[i];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }

        boolean backgroundIsWhite = isBackgroundWhite(grayImage);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayImage.getRGB(x, y) & 0xFF;
                int newPixel = backgroundIsWhite ? (pixel < threshold) ? 255 : 0 : (pixel > threshold) ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private static BufferedImage detectCannyEdges(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage blurred = applyGaussianBlur(grayImage, 5, 1.4f);
        BufferedImage sobel = applySobelOperator(blurred);
        int[] histogram = new int[256];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = sobel.getRGB(x, y) & 0xFF;
                histogram[pixel]++;
            }
        }

        int highThreshold = findPercentileThreshold(histogram, width * height, 0.8);
        int lowThreshold = (int)(highThreshold * 0.5);
        boolean[][] strongEdges = new boolean[height][width];
        boolean[][] weakEdges = new boolean[height][width];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int pixel = sobel.getRGB(x, y) & 0xFF;

                if (pixel >= highThreshold) {
                    strongEdges[y][x] = true;
                }
                else if (pixel >= lowThreshold) {
                    weakEdges[y][x] = true;
                }
            }
        }

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (weakEdges[y][x]) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (strongEdges[y + dy][x + dx]) {
                                strongEdges[y][x] = true;
                                break;
                            }
                        }

                        if (strongEdges[y][x]) {
                            break;
                        }
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

    private static BufferedImage applySobelOperator(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int[][] kernelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] kernelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = 0, gy = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        gx += pixel * kernelX[ky + 1][kx + 1];
                        gy += pixel * kernelY[ky + 1][kx + 1];
                    }
                }

                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                magnitude = Math.min(255, magnitude);
                int rgb = (magnitude << 16) | (magnitude << 8) | magnitude;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private static BufferedImage applyAdvancedMorphology(BufferedImage binary) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        BufferedImage closed = applyMorphologicalClose(binary, 1);
        BufferedImage opened = applyMorphologicalOpen(closed, 1);

        return fillSmallHoles(opened, Math.max(10, (width * height) / 1000));
    }

    private static BufferedImage applyMorphologicalOpen(BufferedImage binary, int radius) {
        BufferedImage eroded = applyMorphologicalErosion(binary, radius);

        return applyMorphologicalDilation(eroded, radius);
    }

    private static BufferedImage applyMorphologicalErosion(BufferedImage binary, int radius) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean allWhite = true;

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx, ny = y + dy;

                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if ((binary.getRGB(nx, ny) & 0xFF) < 128) {
                                allWhite = false;
                                break;
                            }
                        }
                        else {
                            allWhite = false;
                        }
                    }

                    if (!allWhite) {
                        break;
                    }
                }

                int newPixel = allWhite ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private static BufferedImage applyMorphologicalDilation(BufferedImage binary, int radius) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean hasWhite = false;

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx, ny = y + dy;

                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if ((binary.getRGB(nx, ny) & 0xFF) > 128) {
                                hasWhite = true;
                                break;
                            }
                        }
                    }

                    if (hasWhite) {
                        break;
                    }
                }
                int newPixel = hasWhite ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private static BufferedImage fillSmallHoles(BufferedImage binary, int maxHoleSize) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        BufferedImage result = copyImage(binary);
        boolean[][] visited = new boolean[height][width];
        int[] dx = {0, 1, 0, -1};
        int[] dy = {1, 0, -1, 0};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!visited[y][x] && (binary.getRGB(x, y) & 0xFF) < 128) {
                    List<int[]> hole = new ArrayList<>();
                    floodFill(binary, x, y, visited, hole, width, height, dx, dy, false);

                    if (hole.size() <= maxHoleSize) {
                        for (int[] p : hole) {
                            int rgb = (255 << 16) | (255 << 8) | 255;
                            result.setRGB(p[0], p[1], rgb);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static BufferedImage applyAdaptiveThresholdEnhanced(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int blockSize = Math.max(15, Math.min(width, height) / 20);
        int constant = 10;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sum = 0;
                int count = 0;

                for (int dy = -blockSize/2; dy <= blockSize/2; dy++) {
                    for (int dx = -blockSize/2; dx <= blockSize/2; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            sum += grayImage.getRGB(nx, ny) & 0xFF;
                            count++;
                        }
                    }
                }

                int mean = count > 0 ? sum / count : 128;
                int pixel = grayImage.getRGB(x, y) & 0xFF;
                boolean backgroundIsWhite = isBackgroundWhite(grayImage);
                int newPixel = backgroundIsWhite ? (pixel < mean - constant) ? 255 : 0 : (pixel > mean + constant) ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return applyAdvancedMorphology(result);
    }

    private static BufferedImage applyMultiScaleSegmentation(BufferedImage grayImage) {
        BufferedImage fine = applyOtsuWithEdgeRefinement(grayImage);
        BufferedImage coarse = applyAdaptiveThresholdEnhanced(grayImage);
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int finePixel = fine.getRGB(x, y) & 0xFF;
                int coarsePixel = coarse.getRGB(x, y) & 0xFF;
                int newPixel = (finePixel > 128 || coarsePixel > 128) ? 255 : 0;
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return applyAdvancedMorphology(result);
    }

    private static BufferedImage applyContrastEnhancementSegmentation(BufferedImage grayImage) {
        BufferedImage enhanced = enhanceContrast(grayImage);

        return applyOtsuWithEdgeRefinement(enhanced);
    }

    private static BufferedImage enhanceContrast(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int min = 255, max = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayImage.getRGB(x, y) & 0xFF;

                if (pixel < min) {
                    min = pixel;
                }

                if (pixel > max) {
                    max = pixel;
                }
            }
        }

        if (max == min) {
            return grayImage;
        }

        double scale = 255.0 / (max - min);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = grayImage.getRGB(x, y) & 0xFF;
                int newPixel = (int)((pixel - min) * scale);
                newPixel = Math.max(0, Math.min(255, newPixel));
                int rgb = (newPixel << 16) | (newPixel << 8) | newPixel;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private static List<Object[]> findObjectsWithMorphology(BufferedImage binary) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        List<Object[]> objects = new ArrayList<>();
        boolean[][] visited = new boolean[height][width];
        int[] dx = {0, 1, 0, -1, 1, -1, 1, -1};
        int[] dy = {1, 0, -1, 0, 1, 1, -1, -1};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!visited[y][x] && (binary.getRGB(x, y) & 0xFF) > 128) {
                    List<int[]> pixels = new ArrayList<>();
                    int[] stats = {0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
                    
                    findObjectPixels(binary, x, y, visited, pixels, width, height, dx, dy, stats);
                    
                    Object[] object = {pixels, stats[0], stats[1], stats[2], stats[3], stats[4]};
                    objects.add(object);
                }
            }
        }

        return objects;
    }

    private static List<Object[]> filterObjectsByQuality(List<Object[]> objects, int width, int height) {
        List<Object[]> filtered = new ArrayList<>();
        int minArea = 1;
        int maxArea = (width * height) * 9 / 10;

        for (Object[] obj : objects) {
            int area = (int) obj[1];
            if (area >= minArea && area <= maxArea) {
                double compactness = calculateCompactness(obj);
                Rectangle bounds = getBoundingBox(obj);
                double aspectRatio = (double)bounds.width / bounds.height;

                if (compactness > 0.01 && aspectRatio >= 0.05 && aspectRatio <= 50.0) {
                    filtered.add(obj);
                }
            }
        }

        return filtered;
    }

    private static double calculateCompactness(Object[] obj) {
        int area = (int) obj[1];
        Rectangle bounds = getBoundingBox(obj);
        double perimeter = 2 * (bounds.width + bounds.height);

        return area / (perimeter * perimeter);
    }

    private static Rectangle getBoundingBox(Object[] obj) {
        int minX = (int) obj[2];
        int minY = (int) obj[3];
        int maxX = (int) obj[4];
        int maxY = (int) obj[5];

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static double evaluateSegmentationQuality(List<Object[]> objects, int width, int height) {
        if (objects.isEmpty()) {
            return 0;
        }

        double totalArea = 0;
        double sumCompact = 0;

        for (Object[] obj : objects) {
            totalArea += (int) obj[1];
            sumCompact += calculateCompactness(obj);
        }

        double coverage = totalArea / (width * height);
        double avgCompact = objects.isEmpty() ? 0 : sumCompact / objects.size();

        return coverage * avgCompact * (1.0 / (1.0 + Math.abs(objects.size() - 2)));
    }

    private static void findObjectPixels(BufferedImage image, int startX, int startY, boolean[][] visited, List<int[]> pixels, int width, int height, int[] dx, int[] dy, int[] stats) {
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            pixels.add(p);
            
            stats[0]++;
            stats[1] = Math.min(stats[1], p[0]);
            stats[2] = Math.min(stats[2], p[1]);
            stats[3] = Math.max(stats[3], p[0]);
            stats[4] = Math.max(stats[4], p[1]);

            for (int i = 0; i < dx.length; i++) {
                int nx = p[0] + dx[i];
                int ny = p[1] + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                    !visited[ny][nx] && (image.getRGB(nx, ny) & 0xFF) > 128) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
    }

    private static void floodFill(BufferedImage image, int startX, int startY, boolean[][] visited, List<int[]> component, int width, int height, int[] dx, int[] dy, boolean targetValue) {
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            component.add(p);

            for (int i = 0; i < 4; i++) {
                int nx = p[0] + dx[i];
                int ny = p[1] + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                    !visited[ny][nx] && ((image.getRGB(nx, ny) & 0xFF) > 128) == targetValue) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
    }

    private static BufferedImage createBlankImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        return image;
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        return copy;
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
                float weightSum = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int px = Math.min(Math.max(x + kx, 0), width - 1);
                        int py = Math.min(Math.max(y + ky, 0), height - 1);
                        int pixel = image.getRGB(px, py) & 0xFF;
                        float weight = kernel[(ky + radius) * kernelSize + (kx + radius)];
                        sum += pixel * weight;
                        weightSum += weight;
                    }
                }

                int gray = (int) (sum / weightSum);
                gray = Math.max(0, Math.min(255, gray));
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
                float value = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                kernel[(y + radius) * size + (x + radius)] = value;
                sum += value;
            }
        }

        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }

    private static BufferedImage applyMorphologicalClose(BufferedImage binary, int radius) {
        BufferedImage dilated = applyMorphologicalDilation(binary, radius);

        return applyMorphologicalErosion(dilated, radius);
    }

    private static int findPercentileThreshold(int[] histogram, int total, double percentile) {
        int target = (int) (total * percentile);
        int sum = 0;

        for (int i = 255; i >= 0; i--) {
            sum += histogram[i];

            if (sum >= target) {
                return i;
            }
        }

        return 128;
    }

    private static boolean isBackgroundWhite(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        int borderPixels = 0;
        int whitePixels = 0;

        for (int x = 0; x < width; x++) {
            int topPixel = grayImage.getRGB(x, 0) & 0xFF;
            int bottomPixel = grayImage.getRGB(x, height - 1) & 0xFF;

            if (topPixel > 200) {
                whitePixels++;
            }

            if (bottomPixel > 200) {
                whitePixels++;
            }

            borderPixels += 2;
        }

        for (int y = 0; y < height; y++) {
            int leftPixel = grayImage.getRGB(0, y) & 0xFF;
            int rightPixel = grayImage.getRGB(width - 1, y) & 0xFF;

            if (leftPixel > 200) {
                whitePixels++;
            }

            if (rightPixel > 200) {
                whitePixels++;
            }

            borderPixels += 2;
        }

        return (double) whitePixels / borderPixels > 0.5;
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
