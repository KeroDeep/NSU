import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.ByteArrayInputStream;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static JLabel infoLabel;
    private static List<BufferedImage> originalImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();
    private static FileManager currentFileManager = null;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Feature matching");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                imagePanel = new JPanel(new GridLayout(2, 2, 10, 10));
                imagePanel.setBackground(Color.WHITE);
                imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                infoLabel = new JLabel("", JLabel.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.setBackground(Color.WHITE);
                topPanel.add(controlPanel, BorderLayout.NORTH);
                topPanel.add(infoLabel, BorderLayout.CENTER);
                
                mainFrame.add(topPanel, BorderLayout.NORTH);
                mainFrame.add(imagePanel, BorderLayout.CENTER);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private static JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton openBtn = new JButton("Open file manager");
        JButton clearBtn = new JButton("Clear all");
        
        openBtn.addActionListener(event -> openFileManager());
        clearBtn.addActionListener(event -> clearAll());
        
        panel.add(openBtn);
        panel.add(clearBtn);
        
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
                if (paths.length != 2) {
                    JOptionPane.showMessageDialog(mainFrame, "Please select exactly 2 images", "Wrong number of images", JOptionPane.WARNING_MESSAGE);
                    
                    return;
                }

                loadImages(paths);
            }

            refreshDisplay();
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadImages(String[] paths) {
        originalImages.clear();
        imagePaths.clear();
        
        for (String path : paths) {
            try {
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
                event.printStackTrace();
            }
        }
    }

    private static BufferedImage scaleImageForDisplay(BufferedImage original, Dimension targetSize) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        double widthRatio = (double) targetSize.width / originalWidth;
        double heightRatio = (double) targetSize.height / originalHeight;
        double scaleFactor = Math.min(widthRatio, heightRatio);
        
        int scaledWidth = (int) (originalWidth * scaleFactor);
        int scaledHeight = (int) (originalHeight * scaleFactor);
        
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaled;
    }

    private static void refreshDisplay() {
        imagePanel.removeAll();
        infoLabel.setText("");

        if (originalImages.isEmpty()) {
            return;
        }
        else if (originalImages.size() != 2) {
            return;
        }
        else {
            displayFeatureMatching();
        }

        imagePanel.revalidate();
        imagePanel.repaint();
    }

    private static void displayFeatureMatching() {
        BufferedImage img1 = originalImages.get(0);
        BufferedImage img2 = originalImages.get(1);
        
        Object[] result1 = matchFeaturesMethod1(img1, img2);
        Object[] result2 = matchFeaturesMethod2(img1, img2);
        
        BufferedImage img1WithMatches1 = (BufferedImage) result1[0];
        BufferedImage img2WithMatches1 = (BufferedImage) result1[1];
        int matchesCount1 = (Integer) result1[2];
        
        BufferedImage img1WithMatches2 = (BufferedImage) result2[0];
        BufferedImage img2WithMatches2 = (BufferedImage) result2[1];
        int matchesCount2 = (Integer) result2[2];
        
        BufferedImage img1AllPoints = combinePoints(img1, img1WithMatches1, img1WithMatches2);
        BufferedImage img2AllPoints = combinePoints(img2, img2WithMatches1, img2WithMatches2);
        
        int totalMatches = matchesCount1 + matchesCount2;
        infoLabel.setText("Found " + totalMatches + " matching point pairs between images");
        
        Dimension panelSize = imagePanel.getSize();
        int cellWidth = panelSize.width / 2 - 20;
        int cellHeight = panelSize.height / 2 - 20;
        Dimension imageSize = new Dimension(cellWidth, cellHeight);
        
        BufferedImage scaledImg1 = scaleImageForDisplay(img1, imageSize);
        BufferedImage scaledImg2 = scaleImageForDisplay(img2, imageSize);
        BufferedImage scaledImg1AllPoints = scaleImageForDisplay(img1AllPoints, imageSize);
        BufferedImage scaledImg2AllPoints = scaleImageForDisplay(img2AllPoints, imageSize);
        
        addImageToPanel(scaledImg1, "Original image 1");
        addImageToPanel(scaledImg1AllPoints, "Image 1 with all matched points");
        addImageToPanel(scaledImg2, "Original image 2");
        addImageToPanel(scaledImg2AllPoints, "Image 2 with all matched points");
    }
    
    private static BufferedImage combinePoints(BufferedImage original, BufferedImage img1, BufferedImage img2) {
        BufferedImage result = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        
        g2d.drawImage(img1, 0, 0, null);
        g2d.drawImage(img2, 0, 0, null);
        
        g2d.dispose();

        return result;
    }
    
    private static void addImageToPanel(BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel imageLabel = new JLabel(new ImageIcon(image));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        
        container.add(titleLabel, BorderLayout.NORTH);
        container.add(imageLabel, BorderLayout.CENTER);
        imagePanel.add(container);
    }
    
    private static Object[] matchFeaturesMethod1(BufferedImage img1, BufferedImage img2) {
        Mat mat1 = bufferedImageToMat(img1);
        Mat mat2 = bufferedImageToMat(img2);
        
        Mat gray1 = new Mat(), gray2 = new Mat();
        Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mat2, gray2, Imgproc.COLOR_BGR2GRAY);
        
        SIFT detector = SIFT.create();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();
        
        detector.detectAndCompute(gray1, new Mat(), keypoints1, descriptors1);
        detector.detectAndCompute(gray2, new Mat(), keypoints2, descriptors2);
        
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        List<DMatch> goodMatches = new ArrayList<>();
        
        if (!descriptors1.empty() && !descriptors2.empty()) {
            try {
                matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);
                
                for (MatOfDMatch knnMatch : knnMatches) {
                    DMatch[] matches = knnMatch.toArray();

                    if (matches.length >= 2) {
                        if (matches[0].distance < 0.7 * matches[1].distance) {
                            goodMatches.add(matches[0]);
                        }
                    }
                }
                
                if (goodMatches.size() > 4) {
                    goodMatches = filterWithHomography(keypoints1, keypoints2, goodMatches);
                }
                
            }
            catch (Exception exception) {
                System.out.println("Matching error: " + exception.getMessage());
            }
        }
        
        BufferedImage img1WithMatches = drawMatchedPointsMethod1(img1, keypoints1, goodMatches, true);
        BufferedImage img2WithMatches = drawMatchedPointsMethod1(img2, keypoints2, goodMatches, false);
        
        mat1.release(); mat2.release(); gray1.release(); gray2.release();
        keypoints1.release(); keypoints2.release(); descriptors1.release(); descriptors2.release();
        
        return new Object[]{img1WithMatches, img2WithMatches, goodMatches.size()};
    }
    
    private static Object[] matchFeaturesMethod2(BufferedImage img1, BufferedImage img2) {
        Mat mat1 = bufferedImageToMat(img1);
        Mat mat2 = bufferedImageToMat(img2);

        Mat gray1 = new Mat(), gray2 = new Mat();
        Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mat2, gray2, Imgproc.COLOR_BGR2GRAY);

        SIFT detector = SIFT.create();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        detector.detectAndCompute(gray1, new Mat(), keypoints1, descriptors1);
        detector.detectAndCompute(gray2, new Mat(), keypoints2, descriptors2);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        List<DMatch> goodMatches = new ArrayList<>();

        if (!descriptors1.empty() && !descriptors2.empty()) {
            try {
                matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);

                for (MatOfDMatch knnMatch : knnMatches) {
                    DMatch[] matches = knnMatch.toArray();

                    if (matches.length >= 2) {
                        if (matches[0].distance < 0.85 * matches[1].distance) {
                            goodMatches.add(matches[0]);
                        }
                    }
                }

                if (goodMatches.size() > 4) {
                    goodMatches = robustFilterWithHomography(keypoints1, keypoints2, goodMatches);
                    goodMatches = spatialConsistencyFilter(keypoints1, keypoints2, goodMatches);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        BufferedImage[] matchedPair = drawMatchedPointsPairwise(img1, img2, keypoints1, keypoints2, goodMatches);
        BufferedImage img1WithMatches = matchedPair[0];
        BufferedImage img2WithMatches = matchedPair[1];

        int numMatches = goodMatches.size();

        mat1.release();
        mat2.release();
        gray1.release();
        gray2.release();
        keypoints1.release();
        keypoints2.release();
        descriptors1.release();
        descriptors2.release();

        return new Object[]{img1WithMatches, img2WithMatches, numMatches};
    }
    
    private static List<DMatch> filterWithHomography(MatOfKeyPoint keypoints1, MatOfKeyPoint keypoints2, List<DMatch> matches) {
        if (matches.size() < 4) {
            return matches;
        }
        
        List<org.opencv.core.Point> pts1 = new ArrayList<>();
        List<org.opencv.core.Point> pts2 = new ArrayList<>();
        
        KeyPoint[] kp1 = keypoints1.toArray();
        KeyPoint[] kp2 = keypoints2.toArray();
        
        for (DMatch match : matches) {
            pts1.add(kp1[match.queryIdx].pt);
            pts2.add(kp2[match.trainIdx].pt);
        }
        
        MatOfPoint2f points1 = new MatOfPoint2f(pts1.toArray(new org.opencv.core.Point[0]));
        MatOfPoint2f points2 = new MatOfPoint2f(pts2.toArray(new org.opencv.core.Point[0]));
        
        Mat mask = new Mat();
        Calib3d.findHomography(points1, points2, Calib3d.RANSAC, 5.0, mask);
        
        List<DMatch> inliers = new ArrayList<>();
        if (mask.rows() == matches.size()) {
            byte[] maskData = new byte[(int) mask.total()];
            mask.get(0, 0, maskData);
            
            for (int i = 0; i < maskData.length; i++) {
                if (maskData[i] != 0) {
                    inliers.add(matches.get(i));
                }
            }
        }
        
        points1.release();
        points2.release();
        mask.release();
        
        return inliers;
    }
    
    private static BufferedImage drawMatchedPointsMethod1(BufferedImage original, MatOfKeyPoint keypoints, List<DMatch> matches, boolean isImage1) {
        BufferedImage result = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        
        KeyPoint[] kpArray = keypoints.toArray();
        
        Color[] colors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, 
            Color.ORANGE, Color.PINK, Color.YELLOW, new Color(255, 100, 100),
            new Color(100, 255, 100), new Color(100, 100, 255), new Color(255, 255, 100),
            new Color(255, 100, 255), new Color(100, 255, 255), new Color(200, 100, 100),
            new Color(100, 200, 100), new Color(100, 100, 200), new Color(200, 200, 100)
        };
        
        g2d.setStroke(new BasicStroke(2));
        
        for (int i = 0; i < matches.size(); i++) {
            DMatch match = matches.get(i);
            KeyPoint kp = isImage1 ? kpArray[match.queryIdx] : kpArray[match.trainIdx];
            int x = (int) kp.pt.x;
            int y = (int) kp.pt.y;
            
            Color pointColor = colors[i % colors.length];
            g2d.setColor(pointColor);
            
            int pointSize = Math.max(6, Math.min(12, original.getWidth() / 80));
            g2d.fillOval(x - pointSize/2, y - pointSize/2, pointSize, pointSize);
            
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - pointSize/2, y - pointSize/2, pointSize, pointSize);
        }
        
        g2d.dispose();
        
        return result;
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }

        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int mid = sorted.size() / 2;

        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        }
        else {
            return sorted.get(mid);
        }
    }
    
    private static List<DMatch> robustFilterWithHomography(MatOfKeyPoint keypoints1, MatOfKeyPoint keypoints2, List<DMatch> matches) {
        if (matches.size() < 4) {
            return matches;
        }

        List<org.opencv.core.Point> pts1 = new ArrayList<>();
        List<org.opencv.core.Point> pts2 = new ArrayList<>();

        KeyPoint[] kp1 = keypoints1.toArray();
        KeyPoint[] kp2 = keypoints2.toArray();

        for (DMatch match : matches) {
            pts1.add(kp1[match.queryIdx].pt);
            pts2.add(kp2[match.trainIdx].pt);
        }

        MatOfPoint2f points1 = new MatOfPoint2f(pts1.toArray(new org.opencv.core.Point[0]));
        MatOfPoint2f points2 = new MatOfPoint2f(pts2.toArray(new org.opencv.core.Point[0]));

        Mat maskH = new Mat();
        Mat H = Calib3d.findHomography(points1, points2, Calib3d.RANSAC, 3.0, maskH);

        List<DMatch> inliers = new ArrayList<>();

        if (!maskH.empty() && maskH.rows() == matches.size()) {
            byte[] maskData = new byte[(int) maskH.total()];
            maskH.get(0, 0, maskData);

            for (int i = 0; i < maskData.length; i++) {
                if (maskData[i] != 0) {
                    inliers.add(matches.get(i));
                }
            }
        }

        if (inliers.size() < Math.max(4, matches.size() / 3)) {
            Mat maskF = new Mat();
            Mat F = Calib3d.findFundamentalMat(points1, points2, Calib3d.FM_RANSAC, 3.0, 0.99, maskF);

            if (!maskF.empty() && maskF.rows() == matches.size()) {
                byte[] maskDataF = new byte[(int) maskF.total()];
                maskF.get(0, 0, maskDataF);
                List<DMatch> inliersF = new ArrayList<>();

                for (int i = 0; i < maskDataF.length; i++) {
                    if (maskDataF[i] != 0) {
                        inliersF.add(matches.get(i));
                    }
                }

                if (inliersF.size() > inliers.size()) {
                    inliers = inliersF;
                }
            }

            if (maskF != null) {
                maskF.release();
            }

            if (F != null) {
                F.release();
            }
        }

        points1.release();
        points2.release();

        if (maskH != null) {
            maskH.release();
        }

        if (H != null) {
            H.release();
        }

        return inliers;
    }
    
    private static List<DMatch> spatialConsistencyFilter(MatOfKeyPoint keypoints1, MatOfKeyPoint keypoints2, List<DMatch> matches) {
        if (matches.size() < 3) {
            return matches;
        }

        KeyPoint[] kp1 = keypoints1.toArray();
        KeyPoint[] kp2 = keypoints2.toArray();
        List<Double> dx = new ArrayList<>();
        List<Double> dy = new ArrayList<>();

        for (DMatch m : matches) {
            org.opencv.core.Point p1 = kp1[m.queryIdx].pt;
            org.opencv.core.Point p2 = kp2[m.trainIdx].pt;
            dx.add(p2.x - p1.x);
            dy.add(p2.y - p1.y);
        }

        double medianDx = median(dx);
        double medianDy = median(dy);
        List<DMatch> filtered = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            DMatch m = matches.get(i);
            org.opencv.core.Point p1 = kp1[m.queryIdx].pt;
            org.opencv.core.Point p2 = kp2[m.trainIdx].pt;
            double ddx = Math.abs((p2.x - p1.x) - medianDx);
            double ddy = Math.abs((p2.y - p1.y) - medianDy);

            if (ddx < 50 && ddy < 50) {
                filtered.add(m);
            }
        }

        if (filtered.size() < 4) {
            return matches;
        }

        return filtered;
    }

    private static BufferedImage[] drawMatchedPointsPairwise(BufferedImage img1, BufferedImage img2, MatOfKeyPoint keypoints1, MatOfKeyPoint keypoints2, List<DMatch> matches) {
        BufferedImage result1 = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_RGB);
        BufferedImage result2 = new BufferedImage(img2.getWidth(), img2.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g1 = result1.createGraphics();
        Graphics2D g2 = result2.createGraphics();

        g1.drawImage(img1, 0, 0, null);
        g2.drawImage(img2, 0, 0, null);

        KeyPoint[] kp1 = keypoints1.toArray();
        KeyPoint[] kp2 = keypoints2.toArray();

        int pointSize = Math.max(6, Math.min(12, img1.getWidth() / 80));

        for (int i = 0; i < matches.size(); i++) {
            DMatch m = matches.get(i);
            org.opencv.core.Point p1 = kp1[m.queryIdx].pt;
            org.opencv.core.Point p2 = kp2[m.trainIdx].pt;

            Color c = new Color(50 + (i * 73) % 200, 80 + (i * 97) % 150, 100 + (i * 53) % 155);

            g1.setColor(c);
            g2.setColor(c);

            g1.fillOval((int) p1.x - pointSize / 2, (int) p1.y - pointSize / 2, pointSize, pointSize);
            g2.fillOval((int) p2.x - pointSize / 2, (int) p2.y - pointSize / 2, pointSize, pointSize);

            g1.setColor(Color.BLACK);
            g2.setColor(Color.BLACK);
            g1.drawOval((int) p1.x - pointSize / 2, (int) p1.y - pointSize / 2, pointSize, pointSize);
            g2.drawOval((int) p2.x - pointSize / 2, (int) p2.y - pointSize / 2, pointSize, pointSize);
        }

        g1.dispose();
        g2.dispose();
        
        return new BufferedImage[]{result1, result2};
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
}
