import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class FileManager extends AbstractTableModel implements MouseListener, FocusListener, KeyListener, MouseMotionListener {
    private File currentDir;
    private JFrame fileManagerFrame;
    private JTable fileTable;
    private JTextField currentPathField;
    private JComboBox<String> fileTypeComboBox;
    private JToolBar toolBar;
    private JPanel pathPanel;
    private List<Object[]> rows = new ArrayList<>();
    private final String[] columnNames = {"Name", "Size", "Type", "Modified"};
    
    private java.util.function.Consumer<String> imageSelectedListener;
    private java.util.function.Consumer<String[]> imagesSelectedListener;
    private java.util.function.Consumer<String> folderSelectedListener;
    
    private Stack<Map<String, Object>> undoStack = new Stack<>();
    private String lastValidPath;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "tiff", "raw");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "wmv", "flv", "webm");

    private boolean isDragging = false;
    private Point dragStartPoint;
    private Rectangle selectionRect = new Rectangle();

    public FileManager() {
        this.currentDir = new File(System.getProperty("user.dir"));
        this.lastValidPath = currentDir.getAbsolutePath();
    }

    public void setOnImageSelected(java.util.function.Consumer<String> listener) {
        this.imageSelectedListener = listener;
    }
    
    public void setOnImagesSelected(java.util.function.Consumer<String[]> listener) {
        this.imagesSelectedListener = listener;
    }
    
    public void setOnFolderSelected(java.util.function.Consumer<String> listener) {
        this.folderSelectedListener = listener;
    }

    public void showFileManager() {
        createFileManager();
    }

    private void createFileManager() {
        fileManagerFrame = new JFrame("File manager");
        fileManagerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fileManagerFrame.setLayout(new BorderLayout());
        fileManagerFrame.setSize(1000, 700);

        createToolbar();
        createFileTable();

        fileManagerFrame.addKeyListener(this);
        fileManagerFrame.setFocusable(true);
        
        fileManagerFrame.setLocationRelativeTo(null);
        fileManagerFrame.setVisible(true);

        refreshFileList();
    }

    private void createToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton upButton = createToolbarButton("Up", "Up one level", event -> goUp());
        JButton homeButton = createToolbarButton("Home", "Home directory", event -> goHome());
        JButton newFolderButton = createToolbarButton("Folder+", "Create new folder", event -> createNewFolder());
        JButton newFileButton = createToolbarButton("File+", "Create new file", event -> createNewFile());
        JButton deleteButton = createToolbarButton("Delete", "Delete selected", event -> deleteSelected());
        JButton launchButton = createToolbarButton("Launch", "Launch selected files", event -> launchSelected());
        JButton forwardButton = createToolbarButton("Folder->", "Open folder", event -> openSelectedFolder());
        JButton selectAllButton = createToolbarButton("Select All", "Select all files", event -> selectAllFiles());

        toolBar.add(upButton);
        toolBar.add(homeButton);
        toolBar.add(forwardButton);
        toolBar.addSeparator();
        toolBar.add(newFolderButton);
        toolBar.add(newFileButton);
        toolBar.add(deleteButton);
        toolBar.addSeparator();
        toolBar.add(launchButton);
        toolBar.add(selectAllButton);
        toolBar.addSeparator();

        toolBar.add(new JLabel("File types: "));
        String[] fileTypes = {"all", "jpg", "jpeg", "png", "gif", "bmp", "tiff", "raw", "mp4", "avi", "mov"};
        fileTypeComboBox = new JComboBox<>(fileTypes);
        fileTypeComboBox.setSelectedIndex(0);
        toolBar.add(fileTypeComboBox);

        currentPathField = new JTextField(currentDir.getAbsolutePath());
        currentPathField.setBorder(BorderFactory.createEtchedBorder());
        currentPathField.addActionListener(event -> navigateToPath());
        currentPathField.addFocusListener(this);

        pathPanel = new JPanel(new BorderLayout());
        pathPanel.add(new JLabel("Current path: "), BorderLayout.WEST);
        pathPanel.add(currentPathField, BorderLayout.CENTER);

        fileManagerFrame.add(toolBar, BorderLayout.NORTH);
        fileManagerFrame.add(pathPanel, BorderLayout.SOUTH);
    }

    private JButton createToolbarButton(String text, String tooltip, ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.addActionListener(listener);
        
        return button;
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
        fileTable.addMouseMotionListener(this);
        fileTable.addKeyListener(this);
        
        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.addMouseListener(this);
        scrollPane.addMouseMotionListener(this);
        fileManagerFrame.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void focusGained(FocusEvent event) {
        if (event.getSource() == currentPathField) {
            lastValidPath = currentPathField.getText();
        }
    }

    @Override
    public void focusLost(FocusEvent event) {
        if (event.getSource() == currentPathField) {
            navigateToPath();
        }
    }

    private void navigateToPath() {
        String path = currentPathField.getText().trim();
        File newDir = new File(path);

        if (newDir.exists() && newDir.isDirectory()) {
            currentDir = newDir;
            lastValidPath = path;
            refreshFileList();
        }
        else {
            JOptionPane.showMessageDialog(fileManagerFrame, "Invalid path: " + path, "Error", JOptionPane.ERROR_MESSAGE);
            currentPathField.setText(lastValidPath);
        }
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
    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1) {
            if (isClickOnEmptySpace(event)) {
                fileTable.clearSelection();
            }
        }
        else if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
            int[] selectedRows = fileTable.getSelectedRows();

            if (selectedRows.length > 0) {
                openSelectedItem();
            }
        }
    }

    private boolean isClickOnEmptySpace(MouseEvent event) {
        Point p = event.getPoint();
        int row = fileTable.rowAtPoint(p);

        return row == -1;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger()) {
            showPopup(event);
        }
        else if (event.getButton() == MouseEvent.BUTTON1) {
            Point p = event.getPoint();
            int row = fileTable.rowAtPoint(p);
            
            if (row == -1) {
                isDragging = true;
                dragStartPoint = p;
                selectionRect.setBounds(p.x, p.y, 0, 0);
                fileTable.clearSelection();
            }
            else {
                if (!event.isControlDown()) {
                    fileTable.clearSelection();
                }
                fileTable.addRowSelectionInterval(row, row);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger()) {
            showPopup(event);
        }
        else if (event.getButton() == MouseEvent.BUTTON1 && isDragging) {
            isDragging = false;
            updateSelectionFromRectangle();
            selectionRect.setBounds(0, 0, 0, 0);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (isDragging) {
            Point currentPoint = event.getPoint();
            int x = Math.min(dragStartPoint.x, currentPoint.x);
            int y = Math.min(dragStartPoint.y, currentPoint.y);
            int width = Math.abs(currentPoint.x - dragStartPoint.x);
            int height = Math.abs(currentPoint.y - dragStartPoint.y);
            
            selectionRect.setBounds(x, y, width, height);
            updateSelectionFromRectangle();
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {}

    private void updateSelectionFromRectangle() {
        for (int i = 0; i < fileTable.getRowCount(); i++) {
            Rectangle cellRect = fileTable.getCellRect(i, 0, false);
            if (selectionRect.intersects(cellRect)) {
                fileTable.addRowSelectionInterval(i, i);
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent event) {}

    @Override
    public void mouseExited(MouseEvent event) {}

    private void showPopup(MouseEvent event) {
        Component comp = event.getComponent();
        Point p = event.getPoint();

        if (comp instanceof JTable || comp instanceof JScrollPane) {
            int row = fileTable.rowAtPoint(p);

            if (row >= 0) {
                if (!fileTable.isRowSelected(row)) {
                    fileTable.setRowSelectionInterval(row, row);
                }
                showItemPopupMenu(event, row);
            }
            else {
                showEmptySpacePopupMenu(event);
            }
        }
        else {
            showEmptySpacePopupMenu(event);
        }
    }

    private void showItemPopupMenu(MouseEvent event, int row) {
        JPopupMenu popup = new JPopupMenu();
        int modelRow = fileTable.convertRowIndexToModel(row);
        String clickedName = (String) getValueAt(modelRow, 0);
        File clickedFile = new File(currentDir, clickedName);
        
        int selectedCount = fileTable.getSelectedRowCount();
        boolean isSingleSelected = selectedCount == 1;
        boolean isFolder = clickedFile.isDirectory();
        boolean isMediaFile = !isFolder && isMediaFile(clickedFile);
        
        List<File> selectedMediaFiles = getSelectedMediaFiles();
        int mediaFilesCount = selectedMediaFiles.size();
        boolean hasMediaFiles = mediaFilesCount > 0;

        if (isFolder) {
            if (isSingleSelected) {
                JMenuItem openItem = new JMenuItem("Open");
                openItem.addActionListener(event_1 -> openSelectedItem());
                popup.add(openItem);
                
                JMenuItem deleteItem = new JMenuItem("Delete");
                deleteItem.addActionListener(event_1 -> deleteSingleFile(clickedName));
                popup.add(deleteItem);
            }
            else {
                JMenuItem openItem = new JMenuItem("Open " + clickedName);
                openItem.addActionListener(event_1 -> openSingleFolder(clickedName));
                popup.add(openItem);
                
                JMenuItem deleteSingleItem = new JMenuItem("Delete " + clickedName);
                deleteSingleItem.addActionListener(event_1 -> deleteSingleFile(clickedName));
                popup.add(deleteSingleItem);
                
                JMenuItem deleteAllItem = new JMenuItem("Delete all (" + selectedCount + " items)");
                deleteAllItem.addActionListener(event_1 -> deleteSelected());
                popup.add(deleteAllItem);
                
                if (hasMediaFiles) {
                    addLaunchMenuItems(popup, selectedMediaFiles, mediaFilesCount);
                }
            }
        }
        else {
            if (isSingleSelected) {
                if (isMediaFile) {
                    JMenuItem launchItem = new JMenuItem("Launch");
                    launchItem.addActionListener(event_1 -> launchSingleFile(clickedName));
                    popup.add(launchItem);
                }
                
                JMenuItem deleteItem = new JMenuItem("Delete");
                deleteItem.addActionListener(event_1 -> deleteSingleFile(clickedName));
                popup.add(deleteItem);
            }
            else {
                if (isMediaFile) {
                    JMenuItem launchSingleItem = new JMenuItem("Launch " + clickedName);
                    launchSingleItem.addActionListener(event_1 -> launchSingleFile(clickedName));
                    popup.add(launchSingleItem);
                }
                
                JMenuItem deleteSingleItem = new JMenuItem("Delete " + clickedName);
                deleteSingleItem.addActionListener(event_1 -> deleteSingleFile(clickedName));
                popup.add(deleteSingleItem);
                
                JMenuItem deleteAllItem = new JMenuItem("Delete all (" + selectedCount + " items)");
                deleteAllItem.addActionListener(event_1 -> deleteSelected());
                popup.add(deleteAllItem);
                
                if (hasMediaFiles) {
                    addLaunchMenuItems(popup, selectedMediaFiles, mediaFilesCount);
                }
            }
        }

        popup.show(event.getComponent(), event.getX(), event.getY());
    }

    private void addLaunchMenuItems(JPopupMenu popup, List<File> mediaFiles, int mediaFilesCount) {
        if (mediaFilesCount == 1) {
            JMenuItem launchSingleItem = new JMenuItem("Launch " + mediaFiles.get(0).getName());
            launchSingleItem.addActionListener(event_1 -> launchSingleFile(mediaFiles.get(0).getName()));
            popup.add(launchSingleItem);
        }
        else {
            JMenuItem launchAllItem = new JMenuItem("Launch all (" + mediaFilesCount + " items)");
            launchAllItem.addActionListener(event_1 -> launchSelectedMediaFiles(mediaFiles));
            popup.add(launchAllItem);
        }
    }

    private void showEmptySpacePopupMenu(MouseEvent event) {
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem newFolderItem = new JMenuItem("New folder");
        newFolderItem.addActionListener(event_1 -> createNewFolder());
        popup.add(newFolderItem);
        
        JMenuItem newFileItem = new JMenuItem("New file");
        newFileItem.addActionListener(event_1 -> createNewFile());
        popup.add(newFileItem);
        
        JMenuItem loadFolderItem = new JMenuItem("Load entire folder");
        loadFolderItem.addActionListener(event_1 -> loadEntireFolder());
        popup.add(loadFolderItem);
        
        if (!undoStack.isEmpty()) {
            popup.addSeparator();
            JMenuItem undoItem = new JMenuItem("Cancel");
            undoItem.addActionListener(event_1 -> undoLastOperation());
            popup.add(undoItem);
        }
        
        popup.show(event.getComponent(), event.getX(), event.getY());
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelected();
        }
        else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
            if (hasSelectedMediaFiles()) {
                launchSelected();
            }
            else {
                openSelectedItem();
            }
        }
        else if ((event.getKeyCode() == KeyEvent.VK_Z) && ((event.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
            undoLastOperation();
        }
        else if ((event.getKeyCode() == KeyEvent.VK_A) && ((event.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
            selectAllFiles();
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {}

    @Override
    public void keyReleased(KeyEvent event) {}

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
                String type = file.isDirectory() ? "folder" : getFileExtension(file.getName());
                String modified = file.isDirectory() && name.equals("..") ? "" : dateFormat.format(new Date(file.lastModified()));

                rows.add(new Object[]{name, size, type, modified});
            }
        }

        currentPathField.setText(currentDir.getAbsolutePath());
        lastValidPath = currentDir.getAbsolutePath();
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

        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "file";
    }

    private void goUp() {
        File parent = currentDir.getParentFile();

        if (parent != null) {
            currentDir = parent;
            refreshFileList();
            notifyFolderSelected();
        }
    }

    private void goHome() {
        currentDir = new File(System.getProperty("user.dir"));
        refreshFileList();
        notifyFolderSelected();
    }

    private void openSelectedFolder() {
        int row = fileTable.getSelectedRow();

        if (row >= 0) {
            String name = (String) getValueAt(fileTable.convertRowIndexToModel(row), 0);

            if (name.equals("..")) {
                goUp();
            }
            else {
                File file = new File(currentDir, name);

                if (file.isDirectory()) {
                    currentDir = file;
                    refreshFileList();
                    notifyFolderSelected();
                }
            }
        }
    }

    private void openSingleFolder(String folderName) {
        File file = new File(currentDir, folderName);

        if (file.isDirectory()) {
            currentDir = file;
            refreshFileList();
            notifyFolderSelected();
        }
    }

    private void openSelectedItem() {
        int[] selectedRows = fileTable.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        int firstSelectedRow = fileTable.convertRowIndexToModel(selectedRows[0]);
        String firstName = (String) getValueAt(firstSelectedRow, 0);
        
        if (firstName.equals("..")) {
            goUp();
            return;
        }

        List<File> selectedMedia = getSelectedMediaFiles();
        if (!selectedMedia.isEmpty()) {
            launchSelectedMediaFiles(selectedMedia);
            return;
        }

        File firstFile = new File(currentDir, firstName);
        
        if (firstFile.isDirectory()) {
            currentDir = firstFile;
            refreshFileList();
            notifyFolderSelected();
        }
    }

    private void launchSelected() {
        int[] selectedRows = fileTable.getSelectedRows();

        if (selectedRows.length == 0) {
            launchByFileType();
            return;
        }

        List<File> selectedMedia = getSelectedMediaFiles();

        if (!selectedMedia.isEmpty()) {
            launchSelectedMediaFiles(selectedMedia);
        }
        else {
            launchByFileType();
        }
    }

    private List<File> getSelectedMediaFiles() {
        List<File> selectedMedia = new ArrayList<>();
        int[] selectedRows = fileTable.getSelectedRows();
        
        for (int viewRow : selectedRows) {
            int modelRow = fileTable.convertRowIndexToModel(viewRow);
            String name = (String) getValueAt(modelRow, 0);
            File file = new File(currentDir, name);

            if (!name.equals("..") && isMediaFile(file)) {
                selectedMedia.add(file);
            }
        }
        return selectedMedia;
    }

    private void launchSelectedMediaFiles(List<File> mediaFiles) {
        String[] paths = mediaFiles.stream().map(File::getAbsolutePath).toArray(String[]::new);
        notifyImagesSelected(paths);
    }

    private void launchSingleFile(String fileName) {
        File file = new File(currentDir, fileName);

        if (isMediaFile(file)) {
            if (imageSelectedListener != null) {
                imageSelectedListener.accept(file.getAbsolutePath());
            }
        }
    }

    private void launchByFileType() {
        String selectedType = (String) fileTypeComboBox.getSelectedItem();
        Set<String> typesToOpen = new HashSet<>();

        if ("all".equals(selectedType)) {
            typesToOpen.addAll(IMAGE_EXTENSIONS);
            typesToOpen.addAll(VIDEO_EXTENSIONS);
        }
        else {
            typesToOpen.add(selectedType);
        }

        File[] files = currentDir.listFiles();

        if (files != null) {
            List<String> foundMedia = new ArrayList<>();

            for (File file : files) {
                if (file.isFile()) {
                    String ext = getFileExtension(file.getName());

                    if (typesToOpen.contains(ext)) {
                        foundMedia.add(file.getAbsolutePath());
                    }
                }
            }

            if (!foundMedia.isEmpty()) {
                notifyImagesSelected(foundMedia.toArray(new String[0]));
            }
        }
    }

    private void createNewFolder() {
        String name = JOptionPane.showInputDialog(fileManagerFrame, "Enter folder name:");

        if (name != null && !name.trim().isEmpty()) {
            File newFolder = new File(currentDir, name.trim());

            if (newFolder.mkdir()) {
                refreshFileList();
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
                }
            }
            catch (Exception exception) {}
        }
    }

    private void deleteSelected() {
        int[] selectedRows = fileTable.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        List<File> filesToDelete = new ArrayList<>();
        List<File> originalLocations = new ArrayList<>();

        for (int viewRow : selectedRows) {
            int modelRow = fileTable.convertRowIndexToModel(viewRow);
            String name = (String) getValueAt(modelRow, 0);

            if (!name.equals("..")) {
                File file = new File(currentDir, name);
                filesToDelete.add(file);
                originalLocations.add(currentDir);
            }
        }

        if (!filesToDelete.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(fileManagerFrame, "Delete " + filesToDelete.size() + " selected item(s)?", "Confirm delete", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                deleteFiles(filesToDelete, originalLocations);
            }
        }
    }

    private void deleteSingleFile(String fileName) {
        File file = new File(currentDir, fileName);
        List<File> files = List.of(file);
        List<File> locations = List.of(currentDir);
        
        int result = JOptionPane.showConfirmDialog(fileManagerFrame, "Delete " + fileName + "?", "Confirm delete", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            deleteFiles(files, locations);
        }
    }

    private void deleteFiles(List<File> files, List<File> originalLocations) {
        List<File> successfullyDeleted = new ArrayList<>();
        List<File> deletionLocations = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            File location = originalLocations.get(i);
            
            if (deleteRecursive(file)) {
                successfullyDeleted.add(file);
                deletionLocations.add(location);
            }
        }

        if (!successfullyDeleted.isEmpty()) {
            Map<String, Object> undoOperation = new HashMap<>();
            undoOperation.put("operation", "delete");
            undoOperation.put("files", new ArrayList<>(successfullyDeleted));
            undoOperation.put("locations", new ArrayList<>(deletionLocations));
            undoStack.push(undoOperation);
            refreshFileList();
        }
    }

    private void undoLastOperation() {
        if (undoStack.isEmpty()) return;

        Map<String, Object> lastOp = undoStack.pop();
        if ("delete".equals(lastOp.get("operation"))) {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) lastOp.get("files");
            @SuppressWarnings("unchecked")
            List<File> locations = (List<File>) lastOp.get("locations");
            
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                File originalLocation = locations.get(i);
                
                try {
                    if (file.isDirectory()) {
                        file.mkdirs();
                    }
                    else {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                }
                catch (Exception exception) {}
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

    private boolean isMediaFile(File file) {
        if (file.isDirectory()) {
            return false;
        }

        String ext = getFileExtension(file.getName());

        return IMAGE_EXTENSIONS.contains(ext) || VIDEO_EXTENSIONS.contains(ext);
    }

    private boolean hasSelectedMediaFiles() {
        return !getSelectedMediaFiles().isEmpty();
    }

    private void selectAllFiles() {
        fileTable.selectAll();
    }

    private void loadEntireFolder() {
        File[] files = currentDir.listFiles();

        if (files != null) {
            List<String> mediaFiles = new ArrayList<>();

            for (File file : files) {
                if (file.isFile() && isMediaFile(file)) {
                    mediaFiles.add(file.getAbsolutePath());
                }
            }
            
            if (!mediaFiles.isEmpty()) {
                notifyImagesSelected(mediaFiles.toArray(new String[0]));
            }
            else {
                JOptionPane.showMessageDialog(fileManagerFrame, "No media files found in this folder", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void notifyFolderSelected() {
        if (folderSelectedListener != null) {
            folderSelectedListener.accept(currentDir.getAbsolutePath());
        }
    }

    private void notifyImagesSelected(String[] images) {
        if (imagesSelectedListener != null) {
            imagesSelectedListener.accept(images);
        }
    }

    public void close() {
        if (fileManagerFrame != null) {
            fileManagerFrame.dispose();
        }
    }

    public File getCurrentDirectory() {
        return currentDir;
    }

    public void setCurrentDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            this.currentDir = directory;
            this.lastValidPath = directory.getAbsolutePath();
            
            if (fileManagerFrame != null && fileManagerFrame.isVisible()) {
                refreshFileList();
            }
        }
    }
}
