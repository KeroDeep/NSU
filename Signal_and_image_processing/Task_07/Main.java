import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main extends AbstractTableModel implements MouseListener, FocusListener {
    private File currentDir;
    private JFrame fileManagerFrame;
    private JTable fileTable;
    private JTextField currentPathField;
    private JComboBox<String> fileTypeComboBox;
    private JToolBar toolBar;
    private JPanel pathPanel;
    private List<Object[]> rows = new ArrayList<>();
    private final String[] columnNames = {"Name", "Size", "Type", "Modified"};

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        SwingUtilities.invokeLater(() -> new Main().createFileManager());
    }

    public void createFileManager() {
        currentDir = new File(System.getProperty("user.dir"));

        fileManagerFrame = new JFrame("File manager");
        fileManagerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fileManagerFrame.setLayout(new BorderLayout());
        fileManagerFrame.setSize(1000, 700);

        createToolbar();
        createFileTable();

        fileManagerFrame.setLocationRelativeTo(null);
        fileManagerFrame.setVisible(true);

        refreshFileList();
    }

    private void createToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.addMouseListener(this);

        JButton upButton = new JButton("Up");
        upButton.setToolTipText("Up one level");
        upButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        upButton.addActionListener(e -> goUp());

        JButton homeButton = new JButton("Home");
        homeButton.setToolTipText("Home directory");
        homeButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        homeButton.addActionListener(e -> goHome());

        JButton newFolderButton = new JButton("Folder+");
        newFolderButton.setToolTipText("Create new folder");
        newFolderButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        newFolderButton.addActionListener(e -> createNewFolder());

        JButton newFileButton = new JButton("File+");
        newFileButton.setToolTipText("Create new file");
        newFileButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        newFileButton.addActionListener(e -> createNewFile());

        JButton deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("Delete selected");
        deleteButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        deleteButton.addActionListener(e -> deleteSelected());

        JButton selectButton = new JButton("Run");
        selectButton.setToolTipText("Open selected images");
        selectButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        selectButton.addActionListener(e -> openSelectedImages());

        JButton forwardButton = new JButton("Folder->");
        forwardButton.setToolTipText("Open folder");
        forwardButton.setFont(new Font("Segoe UI emoji", Font.PLAIN, 16));
        forwardButton.addActionListener(e -> openSelectedFolder());

        toolBar.add(upButton);
        toolBar.add(homeButton);
        toolBar.add(forwardButton);
        toolBar.addSeparator();
        toolBar.add(newFolderButton);
        toolBar.add(newFileButton);
        toolBar.add(deleteButton);
        toolBar.addSeparator();

        toolBar.add(new JLabel("File types: "));
        String[] fileTypes = {"all", "jpg", "jpeg", "png", "gif", "bmp", "tiff", "raw"};
        fileTypeComboBox = new JComboBox<>(fileTypes);
        fileTypeComboBox.setSelectedIndex(0);
        toolBar.add(fileTypeComboBox);

        toolBar.add(selectButton);

        currentPathField = new JTextField(currentDir.getAbsolutePath());
        currentPathField.setBorder(BorderFactory.createEtchedBorder());

        currentPathField.addActionListener(e -> navigateToPath());
        currentPathField.addFocusListener(this);

        pathPanel = new JPanel(new BorderLayout());
        pathPanel.addMouseListener(this);
        pathPanel.add(new JLabel("Current path: "), BorderLayout.WEST);
        pathPanel.add(currentPathField, BorderLayout.CENTER);

        fileManagerFrame.add(toolBar, BorderLayout.NORTH);
        fileManagerFrame.add(pathPanel, BorderLayout.SOUTH);
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (e.getSource() == currentPathField) {
            File testDir = new File(currentPathField.getText());
            if (!testDir.exists() || !testDir.isDirectory()) {
                currentPathField.setText(currentDir.getAbsolutePath());
            }
        }
    }

    private void navigateToPath() {
        String path = currentPathField.getText().trim();
        File newDir = new File(path);

        if (newDir.exists() && newDir.isDirectory()) {
            currentDir = newDir;
            refreshFileList();
        } else {
            JOptionPane.showMessageDialog(fileManagerFrame, "Invalid path: " + path, "Error", JOptionPane.ERROR_MESSAGE);
            currentPathField.setText(currentDir.getAbsolutePath());
        }
    }

    private void createFileTable() {
        fileTable = new JTable(this);

        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileTable.setRowHeight(25);
        fileTable.setAutoCreateRowSorter(true);

        TableColumnModel columnModel = fileTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(300);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(150);

        fileTable.addMouseListener(this);

        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.addMouseListener(this);
        fileManagerFrame.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            openSelectedItem();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private void showPopup(MouseEvent e) {
        Component comp = e.getComponent();
        Point p = e.getPoint();

        if (comp instanceof JTable || comp instanceof JScrollPane) {
            int row = fileTable.rowAtPoint(p);
            if (row >= 0) {
                fileTable.setRowSelectionInterval(row, row);
                String type = (String) getValueAt(row, 2);
                JPopupMenu popup = new JPopupMenu();
                if (type.equals("folder")) {
                    JMenuItem openItem = new JMenuItem("Open");
                    openItem.addActionListener(e1 -> openSelectedItem());
                    popup.add(openItem);
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    deleteItem.addActionListener(e1 -> deleteSelected());
                    popup.add(deleteItem);
                } else if (isImageType(type)) {
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    deleteItem.addActionListener(e1 -> deleteSelected());
                    popup.add(deleteItem);
                    JMenuItem selectItem = new JMenuItem("Select");
                    selectItem.addActionListener(e1 -> openSelectedImages());
                    popup.add(selectItem);
                } else {
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    deleteItem.addActionListener(e1 -> deleteSelected());
                    popup.add(deleteItem);
                }
                popup.show(fileTable, (int) p.x, (int) p.y);
            } else {
                showCreateMenu(e);
            }
        } else {
            showCreateMenu(e);
        }
    }

    private void showCreateMenu(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem newFolderItem = new JMenuItem("New folder");
        newFolderItem.addActionListener(e1 -> createNewFolder());
        popup.add(newFolderItem);
        JMenuItem newFileItem = new JMenuItem("New file");
        newFileItem.addActionListener(e1 -> createNewFile());
        popup.add(newFileItem);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private boolean isImageType(String type) {
        return type.equals("jpg") || type.equals("jpeg") || type.equals("png") || type.equals("gif") || type.equals("bmp") || type.equals("tiff") || type.equals("raw");
    }

    private void refreshFileList() {
        rows.clear();

        if (currentDir.getParentFile() != null) {
            rows.add(new Object[]{"..", "", "", ""});
        }

        File[] files = currentDir.listFiles();

        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                }
                if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                }
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (File file : files) {
                String name = file.getName();
                String size = file.isDirectory() ? "" : formatFileSize(file.length());
                String type = file.isDirectory() ? "folder" : getFileExtension(file.getName()).toLowerCase();
                String modified = file.isDirectory() && name.equals("..") ? "" : dateFormat.format(new Date(file.lastModified()));

                rows.add(new Object[]{name, size, type, modified});
            }
        }

        currentPathField.setText(currentDir.getAbsolutePath());
        fireTableDataChanged();
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1) : "file";
    }

    private void goUp() {
        File parent = currentDir.getParentFile();
        if (parent != null) {
            currentDir = parent;
            refreshFileList();
        }
    }

    private void goHome() {
        currentDir = new File(System.getProperty("user.dir"));
        refreshFileList();
    }

    private void openSelectedFolder() {
        int row = fileTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) getValueAt(row, 0);
            if (name.equals("..")) {
                goUp();
            } else {
                File file = new File(currentDir, name);
                if (file.isDirectory()) {
                    currentDir = file;
                    refreshFileList();
                }
            }
        }
    }

    private void openSelectedItem() {
        int row = fileTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) getValueAt(row, 0);
            File file = new File(currentDir, name);

            if (name.equals("..")) {
                goUp();
            } else if (file.isDirectory()) {
                currentDir = file;
                refreshFileList();
            } else if (isImageFile(file)) {
                createFilterWindow(file.getAbsolutePath());
            }
        }
    }

    private void openSelectedImages() {
        int[] selectedRows = fileTable.getSelectedRows();
        List<File> selectedImages = new ArrayList<>();

        for (int viewRow : selectedRows) {
            int modelRow = fileTable.convertRowIndexToModel(viewRow);
            String name = (String) getValueAt(modelRow, 0);
            File file = new File(currentDir, name);

            if (!name.equals("..") && isImageFile(file)) {
                selectedImages.add(file);
            }
        }

        if (!selectedImages.isEmpty()) {
            for (File image : selectedImages) {
                createFilterWindow(image.getAbsolutePath());
            }
        } else {
            openImagesBySelectedType();
        }
    }

    private void openImagesBySelectedType() {
        String selectedType = (String) fileTypeComboBox.getSelectedItem();
        Set<String> typesToOpen = new HashSet<>();

        switch (selectedType) {
            case "all":
                typesToOpen.addAll(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff", "raw"));
                break;
            case "jpg":
                typesToOpen.add("jpg");
                break;
            case "jpeg":
                typesToOpen.add("jpeg");
                break;
            case "png":
                typesToOpen.add("png");
                break;
            case "gif":
                typesToOpen.add("gif");
                break;
            case "bmp":
                typesToOpen.add("bmp");
                break;
            case "tiff":
                typesToOpen.add("tiff");
                break;
            case "raw":
                typesToOpen.add("raw");
                break;
        }

        File[] files = currentDir.listFiles();
        if (files != null) {
            boolean foundImages = false;
            for (File file : files) {
                if (file.isFile()) {
                    String ext = getFileExtension(file.getName()).toLowerCase();
                    if (typesToOpen.contains(ext)) {
                        createFilterWindow(file.getAbsolutePath());
                        foundImages = true;
                    }
                }
            }

            if (!foundImages) {
                JOptionPane.showMessageDialog(fileManagerFrame, "No images found with selected type: " + selectedType, "No Images", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void createNewFolder() {
        String name = JOptionPane.showInputDialog(fileManagerFrame, "Enter folder name:");
        if (name != null && !name.trim().isEmpty()) {
            File newFolder = new File(currentDir, name.trim());
            if (newFolder.mkdir()) {
                refreshFileList();
            } else {
                JOptionPane.showMessageDialog(fileManagerFrame, "Cannot create folder");
            }
        }
    }

    private void createNewFile() {
        String name = JOptionPane.showInputDialog(fileManagerFrame, "Enter file name with extension:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                File newFile = new File(currentDir, name.trim());
                if (newFile.createNewFile()) {
                    refreshFileList();
                } else {
                    JOptionPane.showMessageDialog(fileManagerFrame, "Cannot create file");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(fileManagerFrame, "Error: " + ex.getMessage());
            }
        }
    }

    private void deleteSelected() {
        int[] selectedRows = fileTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        int result = JOptionPane.showConfirmDialog(fileManagerFrame,
                "Delete " + selectedRows.length + " selected item(s)?", "Confirm delete",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelRow = fileTable.convertRowIndexToModel(selectedRows[i]);
                String name = (String) getValueAt(modelRow, 0);

                if (!name.equals("..")) {
                    File file = new File(currentDir, name);
                    deleteRecursive(file);
                }
            }
            refreshFileList();
        }
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    private boolean isImageFile(File file) {
        if (file.isDirectory()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".bmp") || name.endsWith(".gif") || name.endsWith(".tiff") ||
                name.endsWith(".raw");
    }

    private void createFilterWindow(String imagePath) {
        Mat originalImage = Imgcodecs.imread(imagePath);

        if (originalImage.empty()) {
            JOptionPane.showMessageDialog(null, "Error: Cannot load image!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File imageFile = new File(imagePath);
        String windowTitle = "Image filtering - " + imageFile.getName();
        JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(1400, 900);

        createFilterInterface(frame, originalImage);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void createFilterInterface(JFrame frame, Mat originalImage) {
        JPanel imagePanel = new JPanel(new GridLayout(3, 3, 5, 5));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel originalLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel noisyLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel gaussianLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel medianLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel customLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel sobelXLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel sobelYLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel sobelLabel = new JLabel("Loading...", JLabel.CENTER);
        JLabel laplacianLabel = new JLabel("Loading...", JLabel.CENTER);

        imagePanel.add(createPanelWithTitle(originalLabel, "Original"));
        imagePanel.add(createPanelWithTitle(noisyLabel, "Noisy"));
        imagePanel.add(createPanelWithTitle(gaussianLabel, "Gaussian"));
        imagePanel.add(createPanelWithTitle(medianLabel, "Median"));
        imagePanel.add(createPanelWithTitle(customLabel, "Custom"));
        imagePanel.add(createPanelWithTitle(sobelXLabel, "Sobel X"));
        imagePanel.add(createPanelWithTitle(sobelYLabel, "Sobel Y"));
        imagePanel.add(createPanelWithTitle(sobelLabel, "Sobel combined"));
        imagePanel.add(createPanelWithTitle(laplacianLabel, "Laplacian"));

        JPanel controlPanel = createFilterControlPanel(frame, originalImage,
                originalLabel, noisyLabel, gaussianLabel, medianLabel, customLabel,
                sobelXLabel, sobelYLabel, sobelLabel, laplacianLabel);

        frame.add(imagePanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);

        applyAllFilters(frame, originalImage, originalLabel, noisyLabel, gaussianLabel,
                medianLabel, customLabel, sobelXLabel, sobelYLabel, sobelLabel, laplacianLabel, 5, 5);
    }

    private JPanel createPanelWithTitle(JComponent component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFilterControlPanel(JFrame frame, Mat originalImage,
                                           JLabel originalLabel, JLabel noisyLabel, JLabel gaussianLabel,
                                           JLabel medianLabel, JLabel customLabel, JLabel sobelXLabel,
                                           JLabel sobelYLabel, JLabel sobelLabel, JLabel laplacianLabel) {

        final int[] gaussianKernelSize = {5};
        final int[] medianKernelSize = {5};
        final int uniformNoiseLevel = 25;
        final int gaussianNoiseLevel = 25;
        final Mat[] noisyImage = {originalImage.clone()};

        JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Filter controls"));

        JPanel sliderPanel = new JPanel(new FlowLayout());

        final JLabel gaussianKernelValue = new JLabel(String.valueOf(gaussianKernelSize[0]));
        JSlider gaussianKernelSlider = new JSlider(3, 15, gaussianKernelSize[0]);
        gaussianKernelSlider.setSnapToTicks(true);
        gaussianKernelSlider.addChangeListener(e -> {
            gaussianKernelSize[0] = ensureOdd(gaussianKernelSlider.getValue());
            gaussianKernelValue.setText(String.valueOf(gaussianKernelSize[0]));
            applyAllFilters(frame, originalImage, originalLabel, noisyLabel, gaussianLabel,
                    medianLabel, customLabel, sobelXLabel, sobelYLabel, sobelLabel,
                    laplacianLabel, gaussianKernelSize[0], medianKernelSize[0]);
        });

        final JLabel medianKernelValue = new JLabel(String.valueOf(medianKernelSize[0]));
        JSlider medianKernelSlider = new JSlider(3, 15, medianKernelSize[0]);
        medianKernelSlider.setSnapToTicks(true);
        medianKernelSlider.addChangeListener(e -> {
            medianKernelSize[0] = ensureOdd(medianKernelSlider.getValue());
            medianKernelValue.setText(String.valueOf(medianKernelSize[0]));
            applyAllFilters(frame, originalImage, originalLabel, noisyLabel, gaussianLabel,
                    medianLabel, customLabel, sobelXLabel, sobelYLabel, sobelLabel,
                    laplacianLabel, gaussianKernelSize[0], medianKernelSize[0]);
        });

        sliderPanel.add(new JLabel("Gaussian kernel:"));
        sliderPanel.add(gaussianKernelSlider);
        sliderPanel.add(gaussianKernelValue);

        sliderPanel.add(new JLabel("Median kernel:"));
        sliderPanel.add(medianKernelSlider);
        sliderPanel.add(medianKernelValue);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton applyNoiseButton = new JButton("Apply noise");
        JButton resetButton = new JButton("Reset");

        applyNoiseButton.addActionListener(e -> {
            noisyImage[0] = originalImage.clone();

            Mat uniformNoise = new Mat(noisyImage[0].size(), noisyImage[0].type());
            Core.randu(uniformNoise, -uniformNoiseLevel, uniformNoiseLevel);
            Core.add(noisyImage[0], uniformNoise, noisyImage[0]);

            Mat gaussianNoise = new Mat(noisyImage[0].size(), noisyImage[0].type());
            Core.randn(gaussianNoise, 0, gaussianNoiseLevel);
            Core.add(noisyImage[0], gaussianNoise, noisyImage[0]);

            Core.normalize(noisyImage[0], noisyImage[0], 0, 255, Core.NORM_MINMAX);

            applyAllFilters(frame, originalImage, originalLabel, noisyLabel, gaussianLabel,
                    medianLabel, customLabel, sobelXLabel, sobelYLabel, sobelLabel,
                    laplacianLabel, gaussianKernelSize[0], medianKernelSize[0]);
        });

        resetButton.addActionListener(e -> {
            gaussianKernelSize[0] = 5;
            medianKernelSize[0] = 5;
            noisyImage[0] = originalImage.clone();
            gaussianKernelSlider.setValue(5);
            medianKernelSlider.setValue(5);
            applyAllFilters(frame, originalImage, originalLabel, noisyLabel, gaussianLabel,
                    medianLabel, customLabel, sobelXLabel, sobelYLabel, sobelLabel,
                    laplacianLabel, gaussianKernelSize[0], medianKernelSize[0]);
        });

        buttonPanel.add(applyNoiseButton);
        buttonPanel.add(resetButton);

        controlPanel.add(sliderPanel);
        controlPanel.add(buttonPanel);

        return controlPanel;
    }

    private void applyAllFilters(JFrame frame, Mat originalImage,
                                 JLabel originalLabel, JLabel noisyLabel, JLabel gaussianLabel,
                                 JLabel medianLabel, JLabel customLabel, JLabel sobelXLabel,
                                 JLabel sobelYLabel, JLabel sobelLabel, JLabel laplacianLabel,
                                 int gaussianKernelSize, int medianKernelSize) {

        if (originalImage.empty()) {
            return;
        }

        Mat grayscaleImage = new Mat();
        if (originalImage.channels() == 3) {
            Imgproc.cvtColor(originalImage, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            grayscaleImage = originalImage.clone();
        }

        Mat noisyImage = originalImage.clone();
        Mat gaussianFiltered = new Mat();
        Mat medianFiltered = new Mat();
        Mat customFiltered = new Mat();
        Mat sobelX = new Mat();
        Mat sobelY = new Mat();
        Mat sobelCombined = new Mat();
        Mat laplacian = new Mat();

        Imgproc.GaussianBlur(noisyImage, gaussianFiltered, new Size(gaussianKernelSize, gaussianKernelSize), 1.0);

        Imgproc.medianBlur(noisyImage, medianFiltered, medianKernelSize);

        Mat kernel = new Mat(3, 3, CvType.CV_32F);
        float[] data = {0, -1, 0, -1, 5, -1, 0, -1, 0};
        kernel.put(0, 0, data);
        Imgproc.filter2D(originalImage, customFiltered, -1, kernel);

        Imgproc.Sobel(grayscaleImage, sobelX, CvType.CV_16S, 1, 0, 3);
        Imgproc.Sobel(grayscaleImage, sobelY, CvType.CV_16S, 0, 1, 3);

        Mat sobelXAbs = new Mat();
        Mat sobelYAbs = new Mat();
        Core.convertScaleAbs(sobelX, sobelXAbs);
        Core.convertScaleAbs(sobelY, sobelYAbs);
        Core.addWeighted(sobelXAbs, 0.5, sobelYAbs, 0.5, 0, sobelCombined);

        Imgproc.Laplacian(grayscaleImage, laplacian, CvType.CV_16S, 3);
        Mat laplacianAbs = new Mat();
        Core.convertScaleAbs(laplacian, laplacianAbs);

        updateImageLabel(originalLabel, originalImage);
        updateImageLabel(noisyLabel, noisyImage);
        updateImageLabel(gaussianLabel, gaussianFiltered);
        updateImageLabel(medianLabel, medianFiltered);
        updateImageLabel(customLabel, customFiltered);
        updateImageLabel(sobelXLabel, sobelXAbs);
        updateImageLabel(sobelYLabel, sobelYAbs);
        updateImageLabel(sobelLabel, sobelCombined);
        updateImageLabel(laplacianLabel, laplacianAbs);

        frame.repaint();
    }

    private void updateImageLabel(JLabel label, Mat image) {
        if (image.empty()) {
            label.setIcon(null);
            label.setText("No image");
            return;
        }

        BufferedImage bufferedImage = matToBufferedImage(image);
        if (bufferedImage != null) {
            ImageIcon icon = new ImageIcon(bufferedImage.getScaledInstance(300, 200, Image.SCALE_SMOOTH));
            label.setIcon(icon);
            label.setText("");
        } else {
            label.setIcon(null);
            label.setText("Error");
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat.empty()) {
            return null;
        }

        Mat tempMat = new Mat();
        if (mat.depth() != CvType.CV_8U) {
            mat.convertTo(tempMat, CvType.CV_8U);
        } else {
            tempMat = mat;
        }

        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (tempMat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(tempMat, rgbMat, Imgproc.COLOR_BGR2RGB);
            tempMat = rgbMat;
        }

        byte[] buffer = new byte[tempMat.channels() * tempMat.cols() * tempMat.rows()];
        tempMat.get(0, 0, buffer);

        BufferedImage image = new BufferedImage(tempMat.cols(), tempMat.rows(), type);
        image.getRaster().setDataElements(0, 0, tempMat.cols(), tempMat.rows(), buffer);

        return image;
    }

    private int ensureOdd(int value) {
        return (value % 2 == 0) ? value + 1 : value;
    }
}
