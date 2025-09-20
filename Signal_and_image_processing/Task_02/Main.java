import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        try {
            Mat originalImage = Imgcodecs.imread("Input.png");
            
            if (originalImage.empty()) {
                System.out.println("Error: Failed to load image!");
                return;
            }
            
            Mat resultImage = originalImage.clone();
            int width = originalImage.cols();
            int height = originalImage.rows();
            
            Size eyeSize = new Size(width * 0.085, height * 0.065);
            Imgproc.ellipse(resultImage, new RotatedRect(new Point(width * 0.44, height * 0.33), eyeSize, 15), new Scalar(0, 255, 0), 3);
            Imgproc.ellipse(resultImage, new RotatedRect(new Point(width * 0.59, height * 0.33), eyeSize, -15), new Scalar(0, 255, 0), 3);
            
            Point[] nosePoints = {
                new Point(width * 0.515, height * 0.43),
                new Point(width * 0.56, height * 0.385),
                new Point(width * 0.47, height * 0.385)
            };

            MatOfPoint noseMat = new MatOfPoint(nosePoints);
            List<MatOfPoint> noseList = new ArrayList<>();
            noseList.add(noseMat);
            Imgproc.polylines(resultImage, noseList, true, new Scalar(255, 0, 0), 3);
            
            Point[] leftEarPoints = {
                new Point(width * 0.34, height * 0.24),
                new Point(width * 0.325, height * 0.2),
                new Point(width * 0.325, height * 0.145),
                new Point(width * 0.335, height * 0.125),
                new Point(width * 0.345, height * 0.12),
                new Point(width * 0.425, height * 0.165),
                new Point(width * 0.35, height * 0.24)
            };
            
            Point[] rightEarPoints = {
                new Point(width * 0.705, height * 0.24),
                new Point(width * 0.72, height * 0.2),
                new Point(width * 0.72, height * 0.145),
                new Point(width * 0.71, height * 0.125),
                new Point(width * 0.7, height * 0.12),
                new Point(width * 0.62, height * 0.175),
                new Point(width * 0.695, height * 0.24)
            };
            
            MatOfPoint leftEarMat = new MatOfPoint(leftEarPoints);
            MatOfPoint rightEarMat = new MatOfPoint(rightEarPoints);
            
            List<MatOfPoint> earsList = new ArrayList<>();
            earsList.add(leftEarMat);
            earsList.add(rightEarMat);
            
            Imgproc.polylines(resultImage, earsList, true, new Scalar(0, 0, 255), 3);
            
            Imgcodecs.imwrite("Output.png", resultImage);
        }
        catch (Exception error) {
            System.out.println("Error: " + error.getMessage());
        }
    }
}
