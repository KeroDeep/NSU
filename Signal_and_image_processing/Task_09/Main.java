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
    
    private static boolean linesMode = true;
    
    private static int accumulatorThresholdLines = 50;
    private static int minLineLength = 20;
    private static int maxLineGap = 10;
    private static int angleStep = 2;
    private static int distanceStep = 1;
    
    private static int accumulatorThresholdCircles = 20;
    private static int minRadius = 5;
    private static int maxRadius = 100;
    private static double minAspectRatio = 0.3;
    private static double maxAspectRatio = 3.0;

    private static JSlider accumulatorThresholdLinesSlider;
    private static JSlider minLineLengthSlider;
    private static JSlider maxLineGapSlider;
    private static JSlider angleStepSlider;
    private static JSlider distanceStepSlider;
    
    private static JSlider accumulatorThresholdCirclesSlider;
    private static JSlider minRadiusSlider;
    private static JSlider maxRadiusSlider;
    private static JSlider minAspectRatioSlider;
    private static JSlider maxAspectRatioSlider;
    
    private static JButton detectionModeButton;
    private static JButton openBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Line and circle detection");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(1600, 1000);
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
        
        openBtn = new JButton("Open file manager");
        JButton clearBtn = new JButton("Clear all");
        JButton resetBtn = new JButton("Reset parameters");
        detectionModeButton = new JButton("Detection: Lines");

        accumulatorThresholdLinesSlider = new JSlider(10, 300, accumulatorThresholdLines);
        accumulatorThresholdLinesSlider.setMajorTickSpacing(50);
        accumulatorThresholdLinesSlider.setMinorTickSpacing(10);
        accumulatorThresholdLinesSlider.setPaintTicks(true);
        accumulatorThresholdLinesSlider.setPaintLabels(true);

        minLineLengthSlider = new JSlider(5, 300, minLineLength);
        minLineLengthSlider.setMajorTickSpacing(50);
        minLineLengthSlider.setMinorTickSpacing(10);
        minLineLengthSlider.setPaintTicks(true);
        minLineLengthSlider.setPaintLabels(true);

        maxLineGapSlider = new JSlider(1, 50, maxLineGap);
        maxLineGapSlider.setMajorTickSpacing(10);
        maxLineGapSlider.setMinorTickSpacing(5);
        maxLineGapSlider.setPaintTicks(true);
        maxLineGapSlider.setPaintLabels(true);

        angleStepSlider = new JSlider(1, 10, angleStep);
        angleStepSlider.setMajorTickSpacing(2);
        angleStepSlider.setMinorTickSpacing(1);
        angleStepSlider.setPaintTicks(true);
        angleStepSlider.setPaintLabels(true);

        distanceStepSlider = new JSlider(1, 10, distanceStep);
        distanceStepSlider.setMajorTickSpacing(2);
        distanceStepSlider.setMinorTickSpacing(1);
        distanceStepSlider.setPaintTicks(true);
        distanceStepSlider.setPaintLabels(true);

        accumulatorThresholdCirclesSlider = new JSlider(5, 100, accumulatorThresholdCircles);
        accumulatorThresholdCirclesSlider.setMajorTickSpacing(20);
        accumulatorThresholdCirclesSlider.setMinorTickSpacing(5);
        accumulatorThresholdCirclesSlider.setPaintTicks(true);
        accumulatorThresholdCirclesSlider.setPaintLabels(true);

        minRadiusSlider = new JSlider(3, 200, minRadius);
        minRadiusSlider.setMajorTickSpacing(50);
        minRadiusSlider.setMinorTickSpacing(10);
        minRadiusSlider.setPaintTicks(true);
        minRadiusSlider.setPaintLabels(true);

        maxRadiusSlider = new JSlider(20, 500, maxRadius);
        maxRadiusSlider.setMajorTickSpacing(100);
        maxRadiusSlider.setMinorTickSpacing(20);
        maxRadiusSlider.setPaintTicks(true);
        maxRadiusSlider.setPaintLabels(true);

        minAspectRatioSlider = new JSlider(10, 100, (int)(minAspectRatio * 100));
        minAspectRatioSlider.setMajorTickSpacing(20);
        minAspectRatioSlider.setMinorTickSpacing(5);
        minAspectRatioSlider.setPaintTicks(true);
        minAspectRatioSlider.setPaintLabels(true);

        maxAspectRatioSlider = new JSlider(100, 300, (int)(maxAspectRatio * 100));
        maxAspectRatioSlider.setMajorTickSpacing(50);
        maxAspectRatioSlider.setMinorTickSpacing(10);
        maxAspectRatioSlider.setPaintTicks(true);
        maxAspectRatioSlider.setPaintLabels(true);

        accumulatorThresholdLinesSlider.addChangeListener(event -> {
            accumulatorThresholdLines = accumulatorThresholdLinesSlider.getValue();
            refreshDisplay();
        });

        minLineLengthSlider.addChangeListener(event -> {
            minLineLength = minLineLengthSlider.getValue();
            refreshDisplay();
        });

        maxLineGapSlider.addChangeListener(event -> {
            maxLineGap = maxLineGapSlider.getValue();
            refreshDisplay();
        });

        angleStepSlider.addChangeListener(event -> {
            angleStep = angleStepSlider.getValue();
            refreshDisplay();
        });

        distanceStepSlider.addChangeListener(event -> {
            distanceStep = distanceStepSlider.getValue();
            refreshDisplay();
        });

        accumulatorThresholdCirclesSlider.addChangeListener(event -> {
            accumulatorThresholdCircles = accumulatorThresholdCirclesSlider.getValue();
            refreshDisplay();
        });

        minRadiusSlider.addChangeListener(event -> {
            minRadius = minRadiusSlider.getValue();

            if (minRadius >= maxRadius) {
                maxRadius = Math.min(500, minRadius + 10);
                maxRadiusSlider.setValue(maxRadius);
            }

            refreshDisplay();
        });

        maxRadiusSlider.addChangeListener(event -> {
            maxRadius = maxRadiusSlider.getValue();

            if (maxRadius <= minRadius) {
                minRadius = Math.max(3, maxRadius - 10);
                minRadiusSlider.setValue(minRadius);
            }

            refreshDisplay();
        });

        minAspectRatioSlider.addChangeListener(event -> {
            minAspectRatio = minAspectRatioSlider.getValue() / 100.0;

            if (minAspectRatio >= maxAspectRatio) {
                maxAspectRatio = Math.min(3.0, minAspectRatio + 0.1);
                maxAspectRatioSlider.setValue((int)(maxAspectRatio * 100));
            }

            refreshDisplay();
        });

        maxAspectRatioSlider.addChangeListener(event -> {
            maxAspectRatio = maxAspectRatioSlider.getValue() / 100.0;

            if (maxAspectRatio <= minAspectRatio) {
                minAspectRatio = Math.max(0.1, maxAspectRatio - 0.1);
                minAspectRatioSlider.setValue((int)(minAspectRatio * 100));
            }

            refreshDisplay();
        });

        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        resetBtn.addActionListener(event -> resetParameters());
        detectionModeButton.addActionListener(event -> toggleDetectionMode());

        JPanel accThreshLinesLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        accThreshLinesLabelPanel.setPreferredSize(new Dimension(180, 25));
        accThreshLinesLabelPanel.add(new JLabel("Accumulator threshold:"));

        JPanel minLineLengthLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        minLineLengthLabelPanel.setPreferredSize(new Dimension(180, 25));
        minLineLengthLabelPanel.add(new JLabel("Min line length:"));

        JPanel maxLineGapLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxLineGapLabelPanel.setPreferredSize(new Dimension(180, 25));
        maxLineGapLabelPanel.add(new JLabel("Max line gap:"));

        JPanel angleStepLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        angleStepLabelPanel.setPreferredSize(new Dimension(180, 25));
        angleStepLabelPanel.add(new JLabel("Angle step:"));

        JPanel distanceStepLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        distanceStepLabelPanel.setPreferredSize(new Dimension(180, 25));
        distanceStepLabelPanel.add(new JLabel("Distance step:"));

        JPanel accThreshCirclesLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        accThreshCirclesLabelPanel.setPreferredSize(new Dimension(180, 25));
        accThreshCirclesLabelPanel.add(new JLabel("Accumulator threshold:"));

        JPanel minRadiusLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        minRadiusLabelPanel.setPreferredSize(new Dimension(180, 25));
        minRadiusLabelPanel.add(new JLabel("Min radius:"));

        JPanel maxRadiusLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxRadiusLabelPanel.setPreferredSize(new Dimension(180, 25));
        maxRadiusLabelPanel.add(new JLabel("Max radius:"));

        JPanel minAspectRatioLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        minAspectRatioLabelPanel.setPreferredSize(new Dimension(180, 25));
        minAspectRatioLabelPanel.add(new JLabel("Min aspect ratio:"));

        JPanel maxAspectRatioLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxAspectRatioLabelPanel.setPreferredSize(new Dimension(180, 25));
        maxAspectRatioLabelPanel.add(new JLabel("Max aspect ratio:"));

        JPanel accThreshLinesPanel = new JPanel(new BorderLayout());
        accThreshLinesPanel.add(accThreshLinesLabelPanel, BorderLayout.WEST);
        accThreshLinesPanel.add(accumulatorThresholdLinesSlider, BorderLayout.CENTER);

        JPanel minLineLengthPanel = new JPanel(new BorderLayout());
        minLineLengthPanel.add(minLineLengthLabelPanel, BorderLayout.WEST);
        minLineLengthPanel.add(minLineLengthSlider, BorderLayout.CENTER);

        JPanel maxLineGapPanel = new JPanel(new BorderLayout());
        maxLineGapPanel.add(maxLineGapLabelPanel, BorderLayout.WEST);
        maxLineGapPanel.add(maxLineGapSlider, BorderLayout.CENTER);

        JPanel angleStepPanel = new JPanel(new BorderLayout());
        angleStepPanel.add(angleStepLabelPanel, BorderLayout.WEST);
        angleStepPanel.add(angleStepSlider, BorderLayout.CENTER);

        JPanel distanceStepPanel = new JPanel(new BorderLayout());
        distanceStepPanel.add(distanceStepLabelPanel, BorderLayout.WEST);
        distanceStepPanel.add(distanceStepSlider, BorderLayout.CENTER);

        JPanel accThreshCirclesPanel = new JPanel(new BorderLayout());
        accThreshCirclesPanel.add(accThreshCirclesLabelPanel, BorderLayout.WEST);
        accThreshCirclesPanel.add(accumulatorThresholdCirclesSlider, BorderLayout.CENTER);

        JPanel minRadiusPanel = new JPanel(new BorderLayout());
        minRadiusPanel.add(minRadiusLabelPanel, BorderLayout.WEST);
        minRadiusPanel.add(minRadiusSlider, BorderLayout.CENTER);

        JPanel maxRadiusPanel = new JPanel(new BorderLayout());
        maxRadiusPanel.add(maxRadiusLabelPanel, BorderLayout.WEST);
        maxRadiusPanel.add(maxRadiusSlider, BorderLayout.CENTER);

        JPanel minAspectRatioPanel = new JPanel(new BorderLayout());
        minAspectRatioPanel.add(minAspectRatioLabelPanel, BorderLayout.WEST);
        minAspectRatioPanel.add(minAspectRatioSlider, BorderLayout.CENTER);

        JPanel maxAspectRatioPanel = new JPanel(new BorderLayout());
        maxAspectRatioPanel.add(maxAspectRatioLabelPanel, BorderLayout.WEST);
        maxAspectRatioPanel.add(maxAspectRatioSlider, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(openBtn, gbc);
        
        gbc.gridx = 1;
        panel.add(clearBtn, gbc);
        
        gbc.gridx = 2;
        panel.add(resetBtn, gbc);
        
        gbc.gridx = 3;
        panel.add(detectionModeButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 5;
        panel.add(accThreshLinesPanel, gbc);
        
        gbc.gridy = 2;
        panel.add(minLineLengthPanel, gbc);
        
        gbc.gridy = 3;
        panel.add(maxLineGapPanel, gbc);
        
        gbc.gridy = 4;
        panel.add(angleStepPanel, gbc);
        
        gbc.gridy = 5;
        panel.add(distanceStepPanel, gbc);
        
        gbc.gridy = 6;
        panel.add(accThreshCirclesPanel, gbc);
        
        gbc.gridy = 7;
        panel.add(minRadiusPanel, gbc);
        
        gbc.gridy = 8;
        panel.add(maxRadiusPanel, gbc);
        
        gbc.gridy = 9;
        panel.add(minAspectRatioPanel, gbc);
        
        gbc.gridy = 10;
        panel.add(maxAspectRatioPanel, gbc);

        accThreshCirclesPanel.setVisible(false);
        minRadiusPanel.setVisible(false);
        maxRadiusPanel.setVisible(false);
        minAspectRatioPanel.setVisible(false);
        maxAspectRatioPanel.setVisible(false);
        
        return panel;
    }

    private static void toggleDetectionMode() {
        linesMode = !linesMode;

        if (linesMode) {
            detectionModeButton.setText("Detection: Lines");
            setPanelVisibility(true);
        }
        else {
            detectionModeButton.setText("Detection: Ellipses");
            setPanelVisibility(false);
        }

        mainFrame.revalidate();
        refreshDisplay();
    }

    private static void setPanelVisibility(boolean linesMode) {
        Component[] components = ((JPanel)mainFrame.getContentPane().getComponent(0)).getComponents();
        
        accumulatorThresholdLinesSlider.getParent().setVisible(linesMode);
        minLineLengthSlider.getParent().setVisible(linesMode);
        maxLineGapSlider.getParent().setVisible(linesMode);
        angleStepSlider.getParent().setVisible(linesMode);
        distanceStepSlider.getParent().setVisible(linesMode);
        
        accumulatorThresholdCirclesSlider.getParent().setVisible(!linesMode);
        minRadiusSlider.getParent().setVisible(!linesMode);
        maxRadiusSlider.getParent().setVisible(!linesMode);
        minAspectRatioSlider.getParent().setVisible(!linesMode);
        maxAspectRatioSlider.getParent().setVisible(!linesMode);
    }

    private static void resetParameters() {
        if (linesMode) {
            accumulatorThresholdLines = 50;
            minLineLength = 20;
            maxLineGap = 10;
            angleStep = 2;
            distanceStep = 1;
            
            accumulatorThresholdLinesSlider.setValue(accumulatorThresholdLines);
            minLineLengthSlider.setValue(minLineLength);
            maxLineGapSlider.setValue(maxLineGap);
            angleStepSlider.setValue(angleStep);
            distanceStepSlider.setValue(distanceStep);
        }
        else {
            accumulatorThresholdCircles = 20;
            minRadius = 5;
            maxRadius = 100;
            minAspectRatio = 0.3;
            maxAspectRatio = 3.0;
            
            accumulatorThresholdCirclesSlider.setValue(accumulatorThresholdCircles);
            minRadiusSlider.setValue(minRadius);
            maxRadiusSlider.setValue(maxRadius);
            minAspectRatioSlider.setValue((int)(minAspectRatio * 100));
            maxAspectRatioSlider.setValue((int)(maxAspectRatio * 100));
        }
        
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
                        BufferedImage scaledImage = scaleImageForProcessing(colorImage, 400, 300);
                        BufferedImage grayImage = convertToGrayscale(scaledImage);
                        originalColorImages.add(scaledImage);
                        originalGrayImages.add(grayImage);
                        imagePaths.add(path);
                    }
                }
            }
            catch (Exception exception) {
                System.err.println("Error loading image: " + path);
                exception.printStackTrace();
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
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            
            BufferedImage colorOriginal = originalColorImages.get(i);
            BufferedImage grayOriginal = originalGrayImages.get(i);
            
            BufferedImage edges = createEdgeImage(grayOriginal);
            BufferedImage detectionResult = createDetectionResult(colorOriginal, grayOriginal, edges);
            
            addImageToPanel(rowPanel, colorOriginal, "Color original");
            addImageToPanel(rowPanel, grayOriginal, "Gray original");
            addImageToPanel(rowPanel, edges, "Edge detection");
            addImageToPanel(rowPanel, detectionResult, linesMode ? "Detected lines" : "Detected ellipses");
            
            imagePanel.add(rowPanel);
            imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
    }

    private static BufferedImage createEdgeImage(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        BufferedImage edges = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int edgeThreshold = calculateAutoEdgeThreshold(gray);
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = ((gray.getRGB(x-1, y-1) & 0xFF) + 2*(gray.getRGB(x-1, y) & 0xFF) + (gray.getRGB(x-1, y+1) & 0xFF)) - ((gray.getRGB(x+1, y-1) & 0xFF) + 2*(gray.getRGB(x+1, y) & 0xFF) + (gray.getRGB(x+1, y+1) & 0xFF));
                
                int gy = ((gray.getRGB(x-1, y-1) & 0xFF) + 2*(gray.getRGB(x, y-1) & 0xFF) + (gray.getRGB(x+1, y-1) & 0xFF)) - ((gray.getRGB(x-1, y+1) & 0xFF) + 2*(gray.getRGB(x, y+1) & 0xFF) + (gray.getRGB(x+1, y+1) & 0xFF));
                
                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                magnitude = Math.min(255, magnitude);
                
                if (magnitude > edgeThreshold) {
                    edges.setRGB(x, y, 0xFFFFFF);
                }
                else {
                    edges.setRGB(x, y, 0);
                }
            }
        }

        return edges;
    }

    private static int calculateAutoEdgeThreshold(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        long total = 0;
        int count = 0;
        
        for (int y = 1; y < height - 1; y += 2) {
            for (int x = 1; x < width - 1; x += 2) {
                int gx = ((gray.getRGB(x-1, y-1) & 0xFF) + 2*(gray.getRGB(x-1, y) & 0xFF) + (gray.getRGB(x-1, y+1) & 0xFF)) - ((gray.getRGB(x+1, y-1) & 0xFF) + 2*(gray.getRGB(x+1, y) & 0xFF) + (gray.getRGB(x+1, y+1) & 0xFF));
                
                int gy = ((gray.getRGB(x-1, y-1) & 0xFF) + 2*(gray.getRGB(x, y-1) & 0xFF) + (gray.getRGB(x+1, y-1) & 0xFF)) - ((gray.getRGB(x-1, y+1) & 0xFF) + 2*(gray.getRGB(x, y+1) & 0xFF) + (gray.getRGB(x+1, y+1) & 0xFF));
                
                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                total += magnitude;
                count++;
            }
        }
        
        if (count == 0) {
            return 50;
        }

        int average = (int)(total / count);

        return Math.max(30, Math.min(100, average / 2));
    }

    private static BufferedImage createDetectionResult(BufferedImage original, BufferedImage gray, BufferedImage edges) {
        BufferedImage result = copyImage(original);
        
        if (linesMode) {
            return createLineDetectionImage(result, edges);
        }
        else {
            return createEllipseDetectionImage(result, gray, edges);
        }
    }

    private static BufferedImage createLineDetectionImage(BufferedImage result, BufferedImage edges) {
        int width = edges.getWidth();
        int height = edges.getHeight();
        
        Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        
        List<int[]> lines = new ArrayList<>();
        
        for (int y = 0; y < height - minLineLength; y++) {
            for (int x = 0; x < width; x++) {
                if ((edges.getRGB(x, y) & 0xFF) > 128) {
                    for (int angle = 0; angle < 180; angle += angleStep) {
                        double theta = Math.toRadians(angle);
                        int length = 0;
                        int endX = x, endY = y;
                        
                        while (length < minLineLength && endX >= 0 && endX < width && endY >= 0 && endY < height) {
                            endX = (int) (x + length * Math.cos(theta));
                            endY = (int) (y + length * Math.sin(theta));
                            
                            if (endX >= 0 && endX < width && endY >= 0 && endY < height) {
                                if ((edges.getRGB(endX, endY) & 0xFF) > 128) {
                                    length++;
                                }
                                else {
                                    break;
                                }
                            }
                            else {
                                break;
                            }
                        }
                        
                        if (length >= minLineLength) {
                            int finalEndX = (int) (x + length * Math.cos(theta));
                            int finalEndY = (int) (y + length * Math.sin(theta));
                            lines.add(new int[]{x, y, finalEndX, finalEndY, length});
                        }
                    }
                }
            }
        }
        
        for (int[] line : lines) {
            g2d.drawLine(line[0], line[1], line[2], line[3]);
        }
        
        g2d.dispose();

        return result;
    }

    private static BufferedImage createEllipseDetectionImage(BufferedImage result, BufferedImage gray, BufferedImage edges) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        
        Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        
        int autoMinDistance = minRadius * 2;
        List<Object[]> ellipses = new ArrayList<>();
        
        List<int[]> edgePoints = new ArrayList<>();
        
        for (int y = minRadius; y < height - minRadius; y += 2) {
            for (int x = minRadius; x < width - minRadius; x += 2) {
                if ((edges.getRGB(x, y) & 0xFF) > 200) {
                    edgePoints.add(new int[]{x, y});
                }
            }
        }
        
        int maxPoints = Math.min(1000, edgePoints.size());
        for (int i = 0; i < maxPoints; i++) {
            int[] point = edgePoints.get(i);
            int x = point[0];
            int y = point[1];
            
            int radiusStep = Math.max(2, (maxRadius - minRadius) / 8);

            for (int r = minRadius; r <= maxRadius; r += radiusStep) {
                double aspectStep = Math.max(0.1, (maxAspectRatio - minAspectRatio) / 4);

                for (double aspect = minAspectRatio; aspect <= maxAspectRatio; aspect += aspectStep) {
                    
                    int rx = r;
                    int ry = (int) (r * aspect);
                    
                    if (rx < 1 || ry < 1) {
                        continue;
                    }
                    
                    int votes = 0;
                    int totalPoints = 0;
                    
                    for (int angle = 0; angle < 360; angle += 20) {
                        double theta = Math.toRadians(angle);
                        int a = (int) (x + rx * Math.cos(theta));
                        int b = (int) (y + ry * Math.sin(theta));
                        
                        if (a >= 0 && a < width && b >= 0 && b < height) {
                            totalPoints++;

                            if ((edges.getRGB(a, b) & 0xFF) > 150) {
                                votes++;
                            }
                        }
                    }
                    
                    double confidence = (double) votes / totalPoints;
                    if (totalPoints >= 12 && confidence > (accumulatorThresholdCircles + 10) / 100.0) {
                        boolean valid = true;

                        for (Object[] existing : ellipses) {
                            int ex = (int) existing[0];
                            int ey = (int) existing[1];
                            double distance = Math.sqrt(Math.pow(x - ex, 2) + Math.pow(y - ey, 2));

                            if (distance < autoMinDistance) {
                                valid = false;
                                break;
                            }
                        }
                        
                        if (valid) {
                            ellipses.add(new Object[]{x, y, rx, ry, confidence});
                        }
                    }
                }
            }
        }
        
        for (Object[] ellipse : ellipses) {
            if ((double) ellipse[4] > 0.5) {
                int x = (int) ellipse[0];
                int y = (int) ellipse[1];
                int rx = (int) ellipse[2];
                int ry = (int) ellipse[3];
                g2d.drawOval(x - rx, y - ry, rx * 2, ry * 2);
            }
        }
        
        g2d.dispose();

        return result;
    }

    private static BufferedImage copyImage(BufferedImage original) {
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return copy;
    }

    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JLabel imageLabel = new JLabel(new ImageIcon(scaleImage(image, 300, 250)));
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
