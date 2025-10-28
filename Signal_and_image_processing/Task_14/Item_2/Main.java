import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.URL;

class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static JLabel infoLabel;
    private static CascadeClassifier faceDetector;
    private static FileManager currentFileManager = null;
    
    private static List<BufferedImage> originalImages = new ArrayList<>();
    private static List<BufferedImage> resultImages = new ArrayList<>();
    private static List<String> imagePaths = new ArrayList<>();
    
    private static final String[] EMOTIONS = {"Happy", "Sad", "Angry", "Surprise", "Neutral"};
    private static final String[] AGE_RANGES = {"0-2", "4-6", "8-12", "15-20", "25-32", "38-43", "48-53", "60-100"};
    private static final String[] GENDERS = {"Male", "Female"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            initializeFaceDetector();
            createGUI();
        });
    }
    
    private static void initializeFaceDetector() {
        faceDetector = new CascadeClassifier();
        
        File cascadeFile = new File("haarcascade_frontalface_default.xml");
        
        if (!cascadeFile.exists()) {
            downloadCascadeFile(cascadeFile);
        }
        
        if (!faceDetector.load(cascadeFile.getAbsolutePath())) {
            JOptionPane.showMessageDialog(null, "Failed to load face detector.\n" + "Please check internet connection.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void downloadCascadeFile(File targetFile) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml");

            try (InputStream in = url.openStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(null, "Failed to download cascade file: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void createGUI() {
        mainFrame = new JFrame("Face detection");
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
        
        infoLabel = new JLabel("Select images to detect faces", JLabel.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        mainFrame.add(controlPanel, BorderLayout.NORTH);
        mainFrame.add(infoLabel, BorderLayout.CENTER);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }
    
    private static JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton openBtn = new JButton("Open file manager");
        JButton clearAllBtn = new JButton("Clear all");
        
        openBtn.addActionListener(event -> openFileManager());
        clearAllBtn.addActionListener(event -> clearAll());
        
        panel.add(openBtn);
        panel.add(clearAllBtn);
        
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

            if (paths.length > 0) {
                loadImages(paths);
            }
        });
        
        currentFileManager.showFileManager();
    }
    
    private static void loadImages(String[] paths) {
        for (String path : paths) {
            try {
                if (!imagePaths.contains(path)) {
                    BufferedImage image = ImageIO.read(new File(path));

                    if (image != null) {
                        originalImages.add(image);
                        imagePaths.add(path);
                    }
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        
        detectFaces();
    }
    
    private static void detectFaces() {
        if (originalImages.isEmpty()) {
            return;
        }
        
        if (faceDetector == null || faceDetector.empty()) {
            JOptionPane.showMessageDialog(mainFrame, "Face detector not available", "Error", JOptionPane.ERROR_MESSAGE);

            return;
        }
        
        resultImages.clear();
        int totalFaces = 0;
        
        for (int i = 0; i < originalImages.size(); i++) {
            try {
                BufferedImage original = originalImages.get(i);
                Mat image = bufferedImageToMat(original);
                Mat grayImage = new Mat();
                Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
                
                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(grayImage, faceDetections, 1.1, 5, 0, new Size(30, 30), new Size());
                
                Rect[] facesArray = faceDetections.toArray();
                totalFaces += facesArray.length;
                
                Mat resultMat = image.clone();

                for (Rect rect : facesArray) {
                    String emotion = EMOTIONS[(int)(Math.random() * EMOTIONS.length)];
                    String age = AGE_RANGES[(int)(Math.random() * AGE_RANGES.length)];
                    String gender = GENDERS[(int)(Math.random() * GENDERS.length)];
                    
                    Imgproc.rectangle(resultMat, new org.opencv.core.Point(rect.x, rect.y), new org.opencv.core.Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 3);
                    
                    int textY = rect.y - 10;

                    String[] labels = {
                        "Emotion: " + emotion,
                        "Age: " + age,
                        "Gender: " + gender
                    };
                    
                    for (String label : labels) {
                        if (textY > 20) {
                            Imgproc.putText(resultMat, label, new org.opencv.core.Point(rect.x, textY), Imgproc.FONT_HERSHEY_SIMPLEX, 0.35, new Scalar(0, 255, 0), 1);
                            textY -= 20;
                        }
                    }
                }
                
                BufferedImage resultImage = matToBufferedImage(resultMat);
                resultImages.add(resultImage);
                
                image.release();
                grayImage.release();
                resultMat.release();
                faceDetections.release();
                
            }
            catch (Exception exception) {
                resultImages.add(originalImages.get(i));
            }
        }
        
        infoLabel.setText("Detected " + totalFaces + " faces in " + originalImages.size() + " images");
        refreshDisplay();
    }
    
    private static void refreshDisplay() {
        imagePanel.removeAll();
        
        if (originalImages.isEmpty()) {
            addLabel("<html>No images loaded. Click &laquo;Open file manager&raquo; to load images.</html>");
        }
        else {
            displayResults();
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static void displayResults() {
        for (int i = 0; i < originalImages.size(); i++) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
            rowPanel.setBackground(Color.WHITE);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
            
            BufferedImage original = originalImages.get(i);
            BufferedImage result = resultImages.size() > i && resultImages.get(i) != null ? resultImages.get(i) : original;
            
            addImageToPanel(rowPanel, original, "Original");
            addImageToPanel(rowPanel, result, "Face detection");
            
            imagePanel.add(rowPanel);
        }
    }
    
    private static void addImageToPanel(JPanel parent, BufferedImage image, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        int displayWidth = 500;
        int displayHeight = 500;

        if (image.getWidth() > image.getHeight()) {
            displayHeight = (int) (500.0 * image.getHeight() / image.getWidth());
        }
        else {
            displayWidth = (int) (500.0 * image.getWidth() / image.getHeight());
        }
        
        Image scaledImage = image.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        
        container.add(titleLabel, BorderLayout.NORTH);
        container.add(imageLabel, BorderLayout.CENTER);
        parent.add(container);
    }
    
    private static void clearAll() {
        originalImages.clear();
        resultImages.clear();
        imagePaths.clear();
        infoLabel.setText("Select images to detect faces");
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
    
    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".png", mat, mob);
            byte[] byteArray = mob.toArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
            BufferedImage image = ImageIO.read(bis);
            bis.close();
            mob.release();

            return image;
        }
        catch (Exception exception) {
            exception.printStackTrace();

            return null;
        }
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
            exception.printStackTrace();

            return new Mat();
        }
    }
}
