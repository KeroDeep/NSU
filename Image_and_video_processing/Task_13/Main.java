import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.*;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;
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
    private static List<BufferedImage> trainImages = new ArrayList<>();
    private static List<Integer> trainLabels = new ArrayList<>();
    private static List<BufferedImage> testImages = new ArrayList<>();
    private static List<Integer> testLabels = new ArrayList<>();
    private static List<Integer> predictedLabels = new ArrayList<>();
    private static SVM svm;
    private static FileManager currentFileManager = null;
    
    private static int svmType = SVM.C_SVC;
    private static int kernelType = SVM.LINEAR;
    
    private static JTabbedPane kernelTabbedPane;
    
    private static JSlider linearRegularizationSlider;
    private static JSlider rbfRegularizationSlider;
    private static JSlider rbfCoefficientSlider;
    private static JSlider polyRegularizationSlider;
    private static JSlider polyDegreeSlider;
    private static JSlider polyOffsetSlider;
    private static JSlider sigmoidRegularizationSlider;
    private static JSlider sigmoidCoefficientSlider;
    private static JSlider sigmoidOffsetSlider;
    
    private static JButton trainBtn;
    private static JButton testBtn;
    private static JButton loadTrainBtn;
    private static JButton loadTestBtn;
    
    private static boolean isResetting = false;
    private static boolean showInitialMessage = false;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainFrame = new JFrame("Digit recognition");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainFrame.setLayout(new BorderLayout());
                
                JPanel controlPanel = createControlPanel();
                imagePanel = new JPanel(new BorderLayout());
                imagePanel.setBackground(Color.WHITE);
                imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JScrollPane scrollPane = new JScrollPane(imagePanel);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                infoLabel = new JLabel("Load folders with train and test datasets", JLabel.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.setBackground(Color.WHITE);
                topPanel.add(controlPanel, BorderLayout.NORTH);
                topPanel.add(infoLabel, BorderLayout.CENTER);
                
                mainFrame.add(topPanel, BorderLayout.NORTH);
                mainFrame.add(scrollPane, BorderLayout.CENTER);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
                
                refreshDisplay();
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private static JPanel createControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel loadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        loadPanel.setBackground(mainPanel.getBackground());
        
        loadTrainBtn = new JButton("Load train folder");
        loadTestBtn = new JButton("Load test folder");
        JButton clearBtn = new JButton("Clear all");
        
        loadPanel.add(loadTrainBtn);
        loadPanel.add(loadTestBtn);
        loadPanel.add(clearBtn);
        
        JPanel kernelPanel = new JPanel(new BorderLayout());
        kernelPanel.setBackground(mainPanel.getBackground());
        kernelPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        kernelTabbedPane = new JTabbedPane();
        kernelTabbedPane.setBackground(mainPanel.getBackground());
        
        JPanel linearPanel = createLinearPanel();
        linearPanel.setBackground(mainPanel.getBackground());
        kernelTabbedPane.addTab("Linear", linearPanel);
        
        JPanel rbfPanel = createRBFPanel();
        rbfPanel.setBackground(mainPanel.getBackground());
        kernelTabbedPane.addTab("Radial", rbfPanel);
        
        JPanel polyPanel = createPolynomialPanel();
        polyPanel.setBackground(mainPanel.getBackground());
        kernelTabbedPane.addTab("Polynomial", polyPanel);
        
        JPanel sigmoidPanel = createSigmoidPanel();
        sigmoidPanel.setBackground(mainPanel.getBackground());
        kernelTabbedPane.addTab("Sigmoid", sigmoidPanel);
        
        kernelPanel.add(kernelTabbedPane, BorderLayout.CENTER);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        actionPanel.setBackground(mainPanel.getBackground());
        
        trainBtn = new JButton("Train model");
        testBtn = new JButton("Test recognition");
        JButton resetBtn = new JButton("Reset parameters");
        
        trainBtn.setEnabled(false);
        testBtn.setEnabled(false);
        
        actionPanel.add(trainBtn);
        actionPanel.add(testBtn);
        actionPanel.add(resetBtn);
        
        addSliderListeners();
        
        loadTrainBtn.addActionListener(event -> openFileManagerForTrain());
        loadTestBtn.addActionListener(event -> openFileManagerForTest());
        trainBtn.addActionListener(event -> trainModel());
        testBtn.addActionListener(event -> testRecognition());
        clearBtn.addActionListener(event -> clearAll());
        resetBtn.addActionListener(event -> {
            resetAllParameters();
            if (trainImages.isEmpty() && testImages.isEmpty()) {
                showInitialMessage = true;
                refreshDisplay();
            }
        });
        
        mainPanel.add(loadPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(kernelPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(actionPanel);
        
        resetAllParameters();
        
        return mainPanel;
    }
    
    private static void addSliderListeners() {
        java.awt.event.ActionListener sliderListener = event -> {
            if (!isResetting && (trainImages.isEmpty() && testImages.isEmpty())) {
                showInitialMessage = true;
                refreshDisplay();
            }
        };
        
        linearRegularizationSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        rbfRegularizationSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        rbfCoefficientSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        polyRegularizationSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        polyDegreeSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        polyOffsetSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        sigmoidRegularizationSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        sigmoidCoefficientSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
        sigmoidOffsetSlider.addChangeListener(event -> sliderListener.actionPerformed(null));
    }
    
    private static JPanel createLinearPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        linearRegularizationSlider = new JSlider(1, 100, 10);
        linearRegularizationSlider.setMajorTickSpacing(20);
        linearRegularizationSlider.setMinorTickSpacing(5);
        linearRegularizationSlider.setPaintTicks(true);
        linearRegularizationSlider.setPaintLabels(true);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Regularization:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(linearRegularizationSlider, gbc);
        
        return panel;
    }
    
    private static JPanel createRBFPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        rbfRegularizationSlider = new JSlider(1, 100, 10);
        rbfRegularizationSlider.setMajorTickSpacing(20);
        rbfRegularizationSlider.setMinorTickSpacing(5);
        rbfRegularizationSlider.setPaintTicks(true);
        rbfRegularizationSlider.setPaintLabels(true);
        
        rbfCoefficientSlider = new JSlider(1, 100, 5);
        rbfCoefficientSlider.setMajorTickSpacing(20);
        rbfCoefficientSlider.setMinorTickSpacing(5);
        rbfCoefficientSlider.setPaintTicks(true);
        rbfCoefficientSlider.setPaintLabels(true);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Regularization:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(rbfRegularizationSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Kernel coefficient:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(rbfCoefficientSlider, gbc);
        
        return panel;
    }
    
    private static JPanel createPolynomialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        polyRegularizationSlider = new JSlider(1, 100, 10);
        polyRegularizationSlider.setMajorTickSpacing(20);
        polyRegularizationSlider.setMinorTickSpacing(5);
        polyRegularizationSlider.setPaintTicks(true);
        polyRegularizationSlider.setPaintLabels(true);
        
        polyDegreeSlider = new JSlider(1, 10, 3);
        polyDegreeSlider.setMajorTickSpacing(3);
        polyDegreeSlider.setMinorTickSpacing(1);
        polyDegreeSlider.setPaintTicks(true);
        polyDegreeSlider.setPaintLabels(true);
        
        polyOffsetSlider = new JSlider(0, 100, 0);
        polyOffsetSlider.setMajorTickSpacing(20);
        polyOffsetSlider.setMinorTickSpacing(5);
        polyOffsetSlider.setPaintTicks(true);
        polyOffsetSlider.setPaintLabels(true);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Regularization:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(polyRegularizationSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Polynomial degree:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(polyDegreeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Kernel offset:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(polyOffsetSlider, gbc);
        
        return panel;
    }
    
    private static JPanel createSigmoidPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        sigmoidRegularizationSlider = new JSlider(1, 100, 1);
        sigmoidRegularizationSlider.setMajorTickSpacing(20);
        sigmoidRegularizationSlider.setMinorTickSpacing(5);
        sigmoidRegularizationSlider.setPaintTicks(true);
        sigmoidRegularizationSlider.setPaintLabels(true);
        
        sigmoidCoefficientSlider = new JSlider(1, 100, 1);
        sigmoidCoefficientSlider.setMajorTickSpacing(20);
        sigmoidCoefficientSlider.setMinorTickSpacing(5);
        sigmoidCoefficientSlider.setPaintTicks(true);
        sigmoidCoefficientSlider.setPaintLabels(true);
        
        sigmoidOffsetSlider = new JSlider(0, 100, 0);
        sigmoidOffsetSlider.setMajorTickSpacing(20);
        sigmoidOffsetSlider.setMinorTickSpacing(5);
        sigmoidOffsetSlider.setPaintTicks(true);
        sigmoidOffsetSlider.setPaintLabels(true);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Regularization:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(sigmoidRegularizationSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Kernel coefficient:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(sigmoidCoefficientSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Kernel offset:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(sigmoidOffsetSlider, gbc);
        
        return panel;
    }
    
    private static void resetAllParameters() {
        isResetting = true;
        
        linearRegularizationSlider.setValue(10);
        rbfRegularizationSlider.setValue(10);
        rbfCoefficientSlider.setValue(5);
        polyRegularizationSlider.setValue(10);
        polyDegreeSlider.setValue(3);
        polyOffsetSlider.setValue(0);
        sigmoidRegularizationSlider.setValue(1);
        sigmoidCoefficientSlider.setValue(1);
        sigmoidOffsetSlider.setValue(0);
        
        isResetting = false;
    }
    
    private static double getCurrentRegularizationParameter() {
        int selectedTab = kernelTabbedPane.getSelectedIndex();
        switch (selectedTab) {
            case 0: return linearRegularizationSlider.getValue() / 10.0;
            case 1: return rbfRegularizationSlider.getValue() / 10.0;
            case 2: return polyRegularizationSlider.getValue() / 10.0;
            case 3: return sigmoidRegularizationSlider.getValue() / 10.0;
            default: return 1.0;
        }
    }
    
    private static double getCurrentKernelCoefficient() {
        int selectedTab = kernelTabbedPane.getSelectedIndex();
        switch (selectedTab) {
            case 1: return rbfCoefficientSlider.getValue() / 100.0;
            case 3: return sigmoidCoefficientSlider.getValue() / 100.0;
            default: return 0.1;
        }
    }
    
    private static double getCurrentPolynomialDegree() {
        return polyDegreeSlider.getValue();
    }
    
    private static double getCurrentKernelOffset() {
        int selectedTab = kernelTabbedPane.getSelectedIndex();
        switch (selectedTab) {
            case 2: return polyOffsetSlider.getValue() / 10.0;
            case 3: return sigmoidOffsetSlider.getValue() / 10.0;
            default: return 0.0;
        }
    }

    private static void openFileManagerForTrain() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnFolderSelected(folderPath -> {
            currentFileManager.close();
            currentFileManager = null;

            if (folderPath != null) {
                File folder = new File(folderPath);
                loadDataset(folder, true);
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void openFileManagerForTest() {
        if (currentFileManager != null) {
            currentFileManager.close();
        }
        
        currentFileManager = new FileManager();
        
        currentFileManager.setOnFolderSelected(folderPath -> {
            currentFileManager.close();
            currentFileManager = null;

            if (folderPath != null) {
                File folder = new File(folderPath);
                loadDataset(folder, false);
            }
        });
        currentFileManager.showFileManager();
    }
    
    private static void loadDataset(File folder, boolean isTrain) {
        List<BufferedImage> images = isTrain ? trainImages : testImages;
        List<Integer> labels = isTrain ? trainLabels : testLabels;
        
        images.clear();
        labels.clear();
        
        int totalLoaded = loadImagesFromSubfolders(folder, images, labels);
        
        if (totalLoaded > 0) {
            if (isTrain) {
                infoLabel.setText("Loaded " + totalLoaded + " training images from " + folder.getName());
                trainBtn.setEnabled(true);
            }
            else {
                infoLabel.setText("Loaded " + totalLoaded + " test images from " + folder.getName());
                testBtn.setEnabled(!trainImages.isEmpty());
            }
        }
        else {
            JOptionPane.showMessageDialog(mainFrame, "No images found in the selected folder", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        refreshDisplay();
    }
    
    private static int loadImagesFromSubfolders(File folder, List<BufferedImage> images, List<Integer> labels) {
        int totalLoaded = 0;
        File[] subfolders = folder.listFiles();
        
        if (subfolders == null) return 0;
        
        for (File subfolder : subfolders) {
            if (subfolder.isDirectory()) {
                try {
                    int digit = Integer.parseInt(subfolder.getName());
                    totalLoaded += loadImagesFromFolder(subfolder, digit, images, labels);
                }
                catch (NumberFormatException exception) {}
            }
        }
        
        return totalLoaded;
    }
    
    private static int loadImagesFromFolder(File folder, int digit, List<BufferedImage> images, List<Integer> labels) {
        int loaded = 0;
        File[] files = folder.listFiles();
        
        if (files == null) {
            return 0;
        }
        
        for (File file : files) {
            if (file.isFile() && 
                (file.getName().toLowerCase().endsWith(".png") || 
                 file.getName().toLowerCase().endsWith(".jpg") ||
                 file.getName().toLowerCase().endsWith(".jpeg"))) {
                try {
                    BufferedImage image = ImageIO.read(file);

                    if (image != null) {
                        images.add(image);
                        labels.add(digit);
                        loaded++;
                    }
                }
                catch (Exception exception) {
                    System.err.println("Error loading image: " + file.getAbsolutePath());
                }
            }
        }
        
        return loaded;
    }

    private static void trainModel() {
        if (trainImages.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No training images loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            trainBtn.setEnabled(false);
            
            svm = SVM.create();
            
            int selectedTab = kernelTabbedPane.getSelectedIndex();
            switch (selectedTab) {
                case 0:
                    kernelType = SVM.LINEAR;
                    break;
                case 1:
                    kernelType = SVM.RBF;
                    break;
                case 2:
                    kernelType = SVM.POLY;
                    break;
                case 3:
                    kernelType = SVM.SIGMOID;
                    break;
            }
            
            double regularization = getCurrentRegularizationParameter();
            double coefficient = getCurrentKernelCoefficient();
            double degree = getCurrentPolynomialDegree();
            double offset = getCurrentKernelOffset();
            
            svm.setType(svmType);
            svm.setKernel(kernelType);
            svm.setC(regularization);
            
            if (kernelType == SVM.RBF || kernelType == SVM.SIGMOID) {
                svm.setGamma(coefficient);
            }
            if (kernelType == SVM.POLY || kernelType == SVM.SIGMOID) {
                svm.setCoef0(offset);
            }
            if (kernelType == SVM.POLY) {
                svm.setDegree(degree);
            }
            
            svm.setTermCriteria(new TermCriteria(TermCriteria.MAX_ITER, 100, 1e-3));
            
            Mat trainingData = new Mat(trainImages.size(), 28 * 28, CvType.CV_32F);
            Mat trainingLabelsMat = new Mat(trainImages.size(), 1, CvType.CV_32S);
            
            for (int i = 0; i < trainImages.size(); i++) {
                BufferedImage img = trainImages.get(i);
                Mat processed = preprocessDigitImage(img);
                
                for (int row = 0; row < 28; row++) {
                    for (int col = 0; col < 28; col++) {
                        double[] pixel = processed.get(row, col);
                        trainingData.put(i, row * 28 + col, pixel[0] / 255.0);
                    }
                }
                
                trainingLabelsMat.put(i, 0, trainLabels.get(i));
                processed.release();
            }
            
            boolean success = svm.train(trainingData, Ml.ROW_SAMPLE, trainingLabelsMat);
            
            trainingData.release();
            trainingLabelsMat.release();
            
            if (success) {
                String kernelName = getKernelName(kernelType);
                infoLabel.setText("Model trained successfully on " + trainImages.size() + " images (" + kernelName + ")");
                testBtn.setEnabled(!testImages.isEmpty());
            }
            else {
                infoLabel.setText("Model training failed");
            }
            
            trainBtn.setEnabled(true);
        }
        catch (Exception exception) {
            JOptionPane.showMessageDialog(mainFrame, "Training failed: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            trainBtn.setEnabled(true);
        }
    }
    
    private static String getKernelName(int kernelType) {
        switch (kernelType) {
            case SVM.LINEAR: return "Linear";
            case SVM.RBF: return "Radial";
            case SVM.POLY: return "Polynomial";
            case SVM.SIGMOID: return "Sigmoid";
            default: return "Unknown";
        }
    }
    
    private static void testRecognition() {
        if (svm == null || testImages.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Model not trained or no test images", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        predictedLabels.clear();
        int correct = 0;
        List<Integer> errorIndices = new ArrayList<>();
        
        for (int i = 0; i < testImages.size(); i++) {
            BufferedImage img = testImages.get(i);
            Mat processed = preprocessDigitImage(img);
            Mat feature = extractFeatures(processed);
            feature.convertTo(feature, CvType.CV_32F);
            
            float prediction = svm.predict(feature.reshape(1, 1));
            int predicted = Math.round(prediction);
            predictedLabels.add(predicted);
            
            if (predicted == testLabels.get(i)) {
                correct++;
            }
            else {
                errorIndices.add(i);
            }
            
            processed.release();
            feature.release();
        }
        
        double accuracy = (double)correct / testImages.size() * 100;
        infoLabel.setText(String.format("Accuracy: %.2f%% (%d/%d)", accuracy, correct, testImages.size()));
        
        displayErrorExamples(errorIndices);
    }
    
    private static Mat preprocessDigitImage(BufferedImage image) {
        Mat mat = bufferedImageToMat(image);
        Mat gray = new Mat();
        
        if (mat.channels() > 1) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        }
        else {
            gray = mat.clone();
        }
        
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(20, 20));
        
        Mat padded = new Mat(28, 28, CvType.CV_8UC1, new Scalar(0));
        Mat roi = padded.submat(4, 24, 4, 24);
        resized.copyTo(roi);
        
        Mat binary = new Mat();
        Imgproc.threshold(padded, binary, 128, 255, Imgproc.THRESH_BINARY_INV);
        
        mat.release();
        gray.release();
        resized.release();
        padded.release();
        
        return binary;
    }
    
    private static Mat extractFeatures(Mat digit) {
        Mat features = new Mat(1, 28 * 28, CvType.CV_32F);
        
        for (int i = 0; i < 28; i++) {
            for (int j = 0; j < 28; j++) {
                double[] pixel = digit.get(i, j);
                features.put(0, i * 28 + j, pixel[0] / 255.0);
            }
        }
        
        return features;
    }
    
    private static void displayErrorExamples(List<Integer> errorIndices) {
        imagePanel.removeAll();
        
        if (errorIndices.isEmpty()) {
            JLabel perfectLabel = new JLabel("Perfect recognition! No errors found.", JLabel.CENTER);
            perfectLabel.setFont(new Font("Arial", Font.BOLD, 18));
            perfectLabel.setForeground(Color.GREEN);
            perfectLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imagePanel.add(perfectLabel, BorderLayout.CENTER);
        }
        else {
            Map<Integer, List<Integer>> errorsByDigit = new TreeMap<>();

            for (int i = 0; i <= 9; i++) {
                errorsByDigit.put(i, new ArrayList<>());
            }
            
            for (int idx : errorIndices) {
                int actualDigit = testLabels.get(idx);
                errorsByDigit.get(actualDigit).add(idx);
            }
            
            displayDigitErrors(errorsByDigit);
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
    }
    
    private static void displayDigitErrors(Map<Integer, List<Integer>> errorsByDigit) {
        imagePanel.setLayout(new GridLayout(0, 5, 10, 10));
        
        for (int digit = 0; digit <= 9; digit++) {
            imagePanel.add(createDigitErrorPanel(digit, errorsByDigit.get(digit)));
        }
    }
    
    private static JPanel createDigitErrorPanel(int digit, List<Integer> errorIndices) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        
        JLabel titleLabel = new JLabel("Digit " + digit, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        
        if (errorIndices.isEmpty()) {
            JLabel noErrorLabel = new JLabel("No errors", JLabel.CENTER);
            noErrorLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            contentPanel.add(noErrorLabel, BorderLayout.CENTER);
        } else {
            Random rand = new Random();
            int randomIdx = errorIndices.get(rand.nextInt(errorIndices.size()));
            BufferedImage img = testImages.get(randomIdx);
            int actual = testLabels.get(randomIdx);
            int predicted = predictedLabels.get(randomIdx);
            
            BufferedImage displayImage = scaleImageForDisplay(img, 80);
            JLabel imageLabel = new JLabel(new ImageIcon(displayImage));
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            
            JLabel infoLabel = new JLabel(String.format("Predicted as: %d", predicted), JLabel.CENTER);
            infoLabel.setFont(new Font("Arial", Font.BOLD, 12));
            
            JLabel countLabel = new JLabel(String.format("Total errors: %d", errorIndices.size()), JLabel.CENTER);
            countLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setBackground(Color.WHITE);
            centerPanel.add(imageLabel, BorderLayout.CENTER);
            centerPanel.add(infoLabel, BorderLayout.SOUTH);
            
            contentPanel.add(centerPanel, BorderLayout.CENTER);
            contentPanel.add(countLabel, BorderLayout.SOUTH);
        }
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static BufferedImage scaleImageForDisplay(BufferedImage original, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, size, size, null);
        g2d.dispose();

        return scaled;
    }

    private static void refreshDisplay() {
        imagePanel.removeAll();
        
        if (showInitialMessage && testImages.isEmpty() && trainImages.isEmpty()) {
            imagePanel.setLayout(new BorderLayout());
            
            JLabel messageLabel = new JLabel("Load folders with train and test datasets to begin", JLabel.CENTER);
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            messageLabel.setForeground(Color.BLACK);
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageLabel.setVerticalAlignment(SwingConstants.CENTER);
            
            imagePanel.add(messageLabel, BorderLayout.CENTER);
        }
        else {
            imagePanel.setLayout(new GridLayout(0, 5, 10, 10));
        }
        
        imagePanel.revalidate();
        imagePanel.repaint();
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

    private static void clearAll() {
        trainImages.clear();
        trainLabels.clear();
        testImages.clear();
        testLabels.clear();
        predictedLabels.clear();
        trainBtn.setEnabled(false);
        testBtn.setEnabled(false);
        infoLabel.setText("Load folders with train and test datasets");
        showInitialMessage = false;
        refreshDisplay();
    }
}
