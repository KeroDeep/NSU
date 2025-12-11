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
import org.opencv.ml.SVM;
import org.opencv.ml.Ml;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    private static JFrame mainFrame;
    private static JPanel imagePanel;
    private static JLabel resultLabel;
    private static BufferedImage testImage;
    private static FileManager currentFileManager = null;
    
    private static SVM animalSVM;
    private static boolean isAnimalModelTrained = false;
    private static String animal1Name = "Cats";
    private static String animal2Name = "Dogs";
    private static List<String> animal1TrainingPaths = new ArrayList<>();
    private static List<String> animal2TrainingPaths = new ArrayList<>();
    
    private static JButton loadTrainBtn;
    private static JButton startTrainingBtn;
    private static JButton openFileManagerBtn;
    private static JButton saveModelBtn;
    private static JButton loadModelBtn;

    private static final String MODEL_PATH = "./Animal_classifier_model.xml";

    public static void main(String[] args) {
        initializeAnimalClassifier();
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Animal classification - cats vs dogs");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                imagePanel = new JPanel(new BorderLayout());
                imagePanel.setBackground(Color.WHITE);
                imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JScrollPane scrollPane = new JScrollPane(imagePanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                resultLabel = new JLabel("Load training dataset and test image to start classification", JLabel.CENTER);
                resultLabel.setFont(new Font("Arial", Font.BOLD, 18));
                resultLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
                
                JPanel contentPanel = new JPanel(new BorderLayout());
                contentPanel.add(resultLabel, BorderLayout.NORTH);
                contentPanel.add(scrollPane, BorderLayout.CENTER);
                
                mainFrame.add(controlPanel, BorderLayout.NORTH);
                mainFrame.add(contentPanel, BorderLayout.CENTER);
                
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
                refreshDisplay();
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }
    
    private static void initializeAnimalClassifier() {
        animalSVM = SVM.create();
        animalSVM.setType(SVM.C_SVC);
        animalSVM.setKernel(SVM.RBF);
        animalSVM.setTermCriteria(new TermCriteria(TermCriteria.MAX_ITER + TermCriteria.EPS, 1000, 1e-6));
    }

    private static JPanel createControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        loadTrainBtn = new JButton("Load train folder");
        startTrainingBtn = new JButton("Start training");
        openFileManagerBtn = new JButton("Open file manager");
        saveModelBtn = new JButton("Save model");
        loadModelBtn = new JButton("Load model");
        
        startTrainingBtn.setEnabled(false);
        saveModelBtn.setEnabled(false);
        openFileManagerBtn.setEnabled(false);
        
        mainPanel.add(loadTrainBtn);
        mainPanel.add(startTrainingBtn);
        mainPanel.add(openFileManagerBtn);
        mainPanel.add(saveModelBtn);
        mainPanel.add(loadModelBtn);
        
        loadTrainBtn.addActionListener(event -> loadTrainingDataset());
        startTrainingBtn.addActionListener(event -> performAnimalTraining());
        openFileManagerBtn.addActionListener(event -> openFileManager());
        saveModelBtn.addActionListener(event -> saveModel());
        loadModelBtn.addActionListener(event -> loadModel());
        
        return mainPanel;
    }
    
    private static void loadTrainingDataset() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnFolderSelected(folderPath -> {
            currentFileManager.close();
            currentFileManager = null;

            if (folderPath != null) {
                File datasetFolder = new File(folderPath);
                loadDatasetFromFolder(datasetFolder);
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadDatasetFromFolder(File datasetFolder) {
        animal1TrainingPaths.clear();
        animal2TrainingPaths.clear();
        
        File catsFolder = new File(datasetFolder, "Cats");
        File dogsFolder = new File(datasetFolder, "Dogs");
        
        if (catsFolder.exists() && catsFolder.isDirectory()) {
            loadImagesFromFolder(catsFolder, animal1TrainingPaths);
        }
        
        if (dogsFolder.exists() && dogsFolder.isDirectory()) {
            loadImagesFromFolder(dogsFolder, animal2TrainingPaths);
        }
        
        int totalImages = animal1TrainingPaths.size() + animal2TrainingPaths.size();
        
        if (totalImages > 0) {
            startTrainingBtn.setEnabled(true);
            resultLabel.setText("Loaded " + animal1TrainingPaths.size() + " cat images and " + animal2TrainingPaths.size() + " dog images. Click \"Start training\" to train model.");
        }
        else {
            JOptionPane.showMessageDialog(mainFrame, "No images found in Cats and Dogs folders. Please check the dataset structure.");
        }
    }
    
    private static void loadImagesFromFolder(File folder, List<String> imagePaths) {
        File[] files = folder.listFiles();
        
        if (files == null) return;
        
        for (File file : files) {
            if (file.isFile() && (file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".jpeg") || file.getName().toLowerCase().endsWith(".png"))) {
                imagePaths.add(file.getAbsolutePath());
            }
        }
    }
    
    private static void performAnimalTraining() {
        if (animal1TrainingPaths.isEmpty() || animal2TrainingPaths.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Please load training dataset first");

            return;
        }
        
        try {
            startTrainingBtn.setEnabled(false);
            
            Mat trainingData = new Mat();
            Mat labels = new Mat();
            
            for (String path : animal1TrainingPaths) {
                Mat features = extractAnimalFeatures(path);
                
                if (!features.empty()) {
                    trainingData.push_back(features);
                    labels.push_back(Mat.ones(1, 1, CvType.CV_32S));
                }
            }
            
            for (String path : animal2TrainingPaths) {
                Mat features = extractAnimalFeatures(path);
                
                if (!features.empty()) {
                    trainingData.push_back(features);
                    labels.push_back(Mat.zeros(1, 1, CvType.CV_32S));
                }
            }
            
            if (trainingData.rows() > 0) {
                animalSVM.train(trainingData, Ml.ROW_SAMPLE, labels);
                isAnimalModelTrained = true;
                saveModelBtn.setEnabled(true);
                openFileManagerBtn.setEnabled(true);
                
                String message = "Model trained successfully on " + trainingData.rows() + " images\n" + "Cats: " + animal1TrainingPaths.size() + " images\n" + "Dogs: " + animal2TrainingPaths.size() + " images\n\n" + "Now you can save the model and test images.";
                
                resultLabel.setText("Model trained successfully! You can now save the model and test images.");
                JOptionPane.showMessageDialog(mainFrame, message);
            }
            
            startTrainingBtn.setEnabled(true);
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Training failed: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            startTrainingBtn.setEnabled(true);
        }
    }
    
    private static void saveModel() {
        if (!isAnimalModelTrained) {
            JOptionPane.showMessageDialog(mainFrame, "No trained model to save");

            return;
        }
        
        try {
            animalSVM.save(MODEL_PATH);
            JOptionPane.showMessageDialog(mainFrame, "Model saved successfully to: " + MODEL_PATH + "\n" + "You can load it next time without training.");
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving model: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void loadModel() {
        File modelFile = new File(MODEL_PATH);

        if (!modelFile.exists()) {
            JOptionPane.showMessageDialog(mainFrame, "Model file not found: " + MODEL_PATH + "\n" + "Please train and save the model first.");

            return;
        }
        
        try {
            animalSVM = SVM.load(MODEL_PATH);

            if (animalSVM != null && !animalSVM.empty()) {
                isAnimalModelTrained = true;
                saveModelBtn.setEnabled(true);
                openFileManagerBtn.setEnabled(true);
                
                resultLabel.setText("Model loaded successfully! You can now test images.");
                JOptionPane.showMessageDialog(mainFrame, "Model loaded successfully from: " + MODEL_PATH + "\n" + "You can now classify images without training.");
            }
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error loading model: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static Mat extractAnimalFeatures(String imagePath) {
        Mat image = Imgcodecs.imread(imagePath);
        
        if (image.empty()) {
            return new Mat();
        }
        
        Imgproc.resize(image, image, new Size(128, 128));
        
        List<Float> features = new ArrayList<>();
        
        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
        
        MatOfFloat ranges = new MatOfFloat(0, 180, 0, 256, 0, 256);
        MatOfInt histSize = new MatOfInt(8, 8, 8);
        Mat hist = new Mat();
        List<Mat> images = new ArrayList<>();
        images.add(hsv);
        
        Imgproc.calcHist(images, new MatOfInt(0, 1, 2), new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        
        for (int i = 0; i < hist.rows(); i++) {
            for (int j = 0; j < hist.cols(); j++) {
                for (int k = 0; k < 1; k++) {
                    double[] value = hist.get(i, j);
                    
                    if (value != null && value.length > 0) {
                        features.add((float)value[0]);
                    }
                }
            }
        }
        
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        
        Mat lbp = computeLBP(gray);
        Mat lbpHist = new Mat();
        Imgproc.calcHist(Arrays.asList(lbp), new MatOfInt(0), new Mat(), lbpHist, new MatOfInt(256), new MatOfFloat(0, 256));
        Core.normalize(lbpHist, lbpHist, 0, 1, Core.NORM_MINMAX);
        
        for (int i = 0; i < lbpHist.rows(); i++) {
            double[] value = lbpHist.get(i, 0);
            
            if (value != null) {
                features.add((float)value[0]);
            }
        }
        
        Mat featuresMat = new Mat(1, features.size(), CvType.CV_32F);
        
        for (int i = 0; i < features.size(); i++) {
            featuresMat.put(0, i, features.get(i));
        }
        
        image.release();
        hsv.release();
        gray.release();
        lbp.release();
        lbpHist.release();
        
        return featuresMat;
    }
    
    private static Mat computeLBP(Mat gray) {
        Mat lbp = new Mat(gray.size(), gray.type());
        int radius = 1;
        int neighbors = 8;
        
        for (int i = radius; i < gray.rows() - radius; i++) {
            for (int j = radius; j < gray.cols() - radius; j++) {
                double center = gray.get(i, j)[0];
                int lbpValue = 0;
                
                for (int n = 0; n < neighbors; n++) {
                    double theta = 2 * Math.PI * n / neighbors;
                    int x = (int)(j + radius * Math.cos(theta));
                    int y = (int)(i - radius * Math.sin(theta));
                    
                    double pixel = gray.get(y, x)[0];
                    
                    if (pixel >= center) {
                        lbpValue |= (1 << n);
                    }
                }

                lbp.put(i, j, lbpValue);
            }
        }

        return lbp;
    }
    
    private static void openFileManager() {
        if (!isAnimalModelTrained) {
            JOptionPane.showMessageDialog(mainFrame, "Please train or load the model first");

            return;
        }
        
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnImagesSelected(paths -> {
            currentFileManager.close();
            currentFileManager = null;

            if (paths != null && paths.length > 0) {
                loadTestImage(paths[0]);
            }
        });
        
        currentFileManager.setOnImageSelected(path -> {
            currentFileManager.close();
            currentFileManager = null;

            if (path != null) {
                loadTestImage(path);
            }
        });
        
        currentFileManager.showFileManager();
    }
    
    private static void loadTestImage(String path) {
        try {
            File file = new File(path);
            testImage = ImageIO.read(file);
            classifyTestImage();
            refreshDisplay();
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Error loading image", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void classifyTestImage() {
        if (testImage == null || !isAnimalModelTrained) return;
        
        try {
            Mat imageMat = bufferedImageToMat(testImage);
            Mat features = extractAnimalFeaturesFromMat(imageMat);
            
            if (features.empty()) {
                resultLabel.setText("Error extracting features from test image");

                return;
            }
            
            float response = animalSVM.predict(features);
            String result = response == 1 ? animal1Name : animal2Name;
            
            if (result.equals(animal1Name)) {
                resultLabel.setForeground(new Color(0, 100, 0));
            }
            else {
                resultLabel.setForeground(new Color(0, 0, 150));
            }
            
            resultLabel.setText("Classification result: " + result);
            
            imageMat.release();
            features.release();
            
        }
        catch (Exception exception) {
            resultLabel.setForeground(Color.RED);
            resultLabel.setText("Classification error: " + exception.getMessage());
        }
    }
    
    private static Mat extractAnimalFeaturesFromMat(Mat image) {
        Imgproc.resize(image, image, new Size(128, 128));
        
        List<Float> features = new ArrayList<>();
        
        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
        
        MatOfFloat ranges = new MatOfFloat(0, 180, 0, 256, 0, 256);
        MatOfInt histSize = new MatOfInt(8, 8, 8);
        Mat hist = new Mat();
        List<Mat> images = new ArrayList<>();
        images.add(hsv);
        
        Imgproc.calcHist(images, new MatOfInt(0, 1, 2), new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        
        for (int i = 0; i < hist.rows(); i++) {
            for (int j = 0; j < hist.cols(); j++) {
                for (int k = 0; k < 1; k++) {
                    double[] value = hist.get(i, j);
                    
                    if (value != null && value.length > 0) {
                        features.add((float)value[0]);
                    }
                }
            }
        }
        
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        
        Mat lbp = computeLBP(gray);
        Mat lbpHist = new Mat();
        Imgproc.calcHist(Arrays.asList(lbp), new MatOfInt(0), new Mat(), lbpHist, new MatOfInt(256), new MatOfFloat(0, 256));
        Core.normalize(lbpHist, lbpHist, 0, 1, Core.NORM_MINMAX);
        
        for (int i = 0; i < lbpHist.rows(); i++) {
            double[] value = lbpHist.get(i, 0);
            
            if (value != null) {
                features.add((float)value[0]);
            }
        }
        
        Mat featuresMat = new Mat(1, features.size(), CvType.CV_32F);
        
        for (int i = 0; i < features.size(); i++) {
            featuresMat.put(0, i, features.get(i));
        }
        
        hsv.release();
        gray.release();
        lbp.release();
        lbpHist.release();
        
        return featuresMat;
    }
    
    private static void refreshDisplay() {
        imagePanel.removeAll();
        
        if (testImage != null) {
            JLabel imageLabel = new JLabel(new ImageIcon(scaleImagePreservingAspectRatio(testImage, 600)));
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imagePanel.add(imageLabel, BorderLayout.CENTER);
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static BufferedImage scaleImagePreservingAspectRatio(BufferedImage original, int maxSize) {
        if (original == null) {
            return null;
        }
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return original;
        }
        
        double scale;

        if (width > height) {
            scale = (double) maxSize / width;
        }
        else {
            scale = (double) maxSize / height;
        }
        
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
}
