import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.print("Enter the path to the image: ");
            String imagePath = scanner.nextLine();
            
            Mat colorImage = Imgcodecs.imread(imagePath);

            if (colorImage.empty()) {
                System.out.println("Error: Failed to load image!");
                return;
            }
            
            Mat grayImage = new Mat();
            Imgproc.cvtColor(colorImage, grayImage, Imgproc.COLOR_BGR2GRAY);
            
            int threshold = -1;

            while (threshold < 0 || threshold > 255) {
                System.out.print("Enter value of threshold (0-255): ");

                if (scanner.hasNextInt()) {
                    threshold = scanner.nextInt();

                    if (threshold < 0 || threshold > 255) {
                        System.out.println("Error: Threshold must be between 0 and 255!");
                    }
                }
                else {
                    System.out.println("Error: Please enter a valid integer!");
                    scanner.next();
                }
            }
            
            int count = 0;
            int totalPixels = grayImage.rows() * grayImage.cols();
            
            for (int y = 0; y < grayImage.rows(); y++) {
                for (int x = 0; x < grayImage.cols(); x++) {
                    double brightness = grayImage.get(y, x)[0];

                    if (brightness > threshold) {
                        count++;
                    }
                }
            }
            
            double percentage = (count * 100.0) / totalPixels;
            System.out.println();
            System.out.println("Image Size: " + grayImage.cols() + "x" + grayImage.rows());
            System.out.println("Total number of pixels: " + totalPixels);
            System.out.println("Pixels with brightness > " + threshold + ": " + count);
            System.out.println("Percentage of pixels: " + String.format("%.2f", percentage) + "%");
            System.out.println();
            
            scanner.nextLine();

            String show = "";

            while (!show.equalsIgnoreCase("y") && !show.equalsIgnoreCase("n")) {
                System.out.print("Show images? (y/n): ");
                show = scanner.nextLine().trim();
                
                if (!show.equalsIgnoreCase("y") && !show.equalsIgnoreCase("n")) {
                    System.out.println("Error: Please enter 'y' or 'n'!");
                }
            }

            System.out.println();

            if (show.equalsIgnoreCase("y")) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    showImage("Original image", colorImage);
                    showImage("Black and white image", grayImage);
                });
            }
        }
        catch (Exception error) {
            System.out.println("Error: " + error.getMessage());
        }
        finally {
            scanner.close();
        }
    }
    
    private static void showImage(String title, Mat image) {
        try {
            Mat displayImage = new Mat();

            if (image.channels() == 1) {
                Imgproc.cvtColor(image, displayImage, Imgproc.COLOR_GRAY2BGR);
            }
            else {
                displayImage = image;
            }
            
            javax.swing.JFrame frame = new javax.swing.JFrame(title);
            frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            
            java.awt.Image awtImage = matToBufferedImage(displayImage);
            javax.swing.JLabel label = new javax.swing.JLabel(new javax.swing.ImageIcon(awtImage));
            
            frame.getContentPane().add(label);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
        }
        catch (Exception error) {
            System.out.println("Error displaying image: " + error.getMessage());
        }
    }
    
    private static java.awt.Image matToBufferedImage(Mat mat) {
        int type = java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

        if (mat.channels() > 1) {
            type = java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
            
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
            
            int bufferSize = rgbMat.channels() * rgbMat.cols() * rgbMat.rows();
            byte[] buffer = new byte[bufferSize];
            rgbMat.get(0, 0, buffer);
            
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(rgbMat.cols(), rgbMat.rows(), type);
            
            java.awt.image.WritableRaster raster = image.getRaster();
            raster.setDataElements(0, 0, rgbMat.cols(), rgbMat.rows(), buffer);
            
            return image;
        }
        
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer);
        
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(mat.cols(), mat.rows(), type);
        
        java.awt.image.WritableRaster raster = image.getRaster();
        raster.setDataElements(0, 0, mat.cols(), mat.rows(), buffer);
        
        return image;
    }
}
