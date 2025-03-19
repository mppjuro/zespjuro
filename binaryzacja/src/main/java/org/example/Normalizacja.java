package org.example;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.io.File;
import org.opencv.imgcodecs.Imgcodecs;

public class Normalizacja {

    static {
        try {
            Loader.load(opencv_java.class);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Nie udało się załadować OpenCV.");
        }
    }

    public static BufferedImage normalizeImage(BufferedImage input) {
        if (input == null) throw new IllegalArgumentException("Wejściowy obraz jest null.");

        Mat originalMat = bufferedImageToMat(input);
        Mat bestMat = originalMat.clone();
        double bestAngle = 0;
        final double ANGLE_STEP = 1;

        boolean found = false;
        int bestHorizontal = 0;
        for (double angle = -20.0; angle <= 20.0; angle += ANGLE_STEP) {
            Mat rotated = rotateImage(originalMat, angle);

            Mat redMask = new Mat();
            //Core.inRange(rotated, new Scalar(0, 0, 50), new Scalar(120, 120, 255), redMask);
            Core.inRange(rotated, new Scalar(0, 0, 30), new Scalar(150, 150, 255), redMask);
            Imgproc.GaussianBlur(redMask, redMask, new Size(3, 3), 0);

            Mat lines = new Mat();
            //Imgproc.HoughLinesP(redMask, lines, 1, Math.PI/180, 30, 250, 100);
            Imgproc.HoughLinesP(
                    redMask,
                    lines,
                    1,                  // rho - pozostaw bez zmian
                    Math.PI/360,        // theta - zwiększ czułość kątową
                    20,                 // threshold - niższy próg
                    150,                // minLineLength - krótsze linie
                    50                  // maxLineGap - mniejsza przerwa
            );
            int horizontal = 0;
            int vertical = 0;

            for (int i = 0; i < lines.rows(); i++) {
                double[] line = lines.get(i, 0);
                double x1 = line[0], y1 = line[1], x2 = line[2], y2 = line[3];

                double angleLine = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
                angleLine = Math.abs(angleLine);

                boolean isVertical = (angleLine >= 89 && angleLine <= 91);
                boolean isHorizontal = (angleLine <= 1) || (angleLine >= 179);

                if (isVertical && isLineOnGrid(x1, y1, x2, y2, redMask)) vertical++;
                else if (isHorizontal && isLineOnGrid(x1, y1, x2, y2, redMask)) horizontal++;
            }

            if (horizontal > bestHorizontal) {
                bestAngle = angle + 1;
                bestMat = rotateImage(originalMat, bestAngle);
                bestHorizontal = horizontal;
            }
            System.out.println("Testowanie kąta: " + angle + "°, pionowych: " + vertical + ", poziomych: " + horizontal);
        }

        System.out.println("Optymalny kąt znaleziony: " + bestAngle + "°");

        // Zamiana wszystkich czarnych pikseli na białe
        for (int y = 0; y < bestMat.rows(); y++) {
            for (int x = 0; x < bestMat.cols(); x++) {
                double[] pixel = bestMat.get(y, x);
                if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                    bestMat.put(y, x, new double[]{255, 255, 255});
                }
            }
        }

        // Przycinanie białych marginesów
        Rect boundingRect = getBoundingRect(bestMat);
        bestMat = new Mat(bestMat, boundingRect);

        if (bestAngle < 1 && bestAngle > -1) {
            bestMat = originalMat.clone();
        }

        return matToBufferedImage(bestMat);
    }

    private static Rect getBoundingRect(Mat mat) {
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 250, 255, Imgproc.THRESH_BINARY_INV);

        Mat points = new Mat();
        Core.findNonZero(binary, points);
        return Imgproc.boundingRect(points);
    }

    private static Mat rotateImage(Mat src, double angle) {
        Mat dst = new Mat();
        Point center = new Point(src.cols()/2.0, src.rows()/2.0);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        Rect bbox = new RotatedRect(center, src.size(), angle).boundingRect();
        rotationMatrix.put(0, 2, rotationMatrix.get(0, 2)[0] + bbox.width/2.0 - center.x);
        rotationMatrix.put(1, 2, rotationMatrix.get(1, 2)[0] + bbox.height/2.0 - center.y);

        Imgproc.warpAffine(src, dst, rotationMatrix, bbox.size());
        return dst;
    }

    private static int[] countLines(Mat lines, Mat mask) {
        int horizontal = 0;
        int vertical = 0;

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            double x1 = line[0], y1 = line[1], x2 = line[2], y2 = line[3];

            double angle = Math.toDegrees(Math.atan2(y2-y1, x2-x1));
            angle = Math.abs(angle);

            boolean isVertical = (angle >= 89.85 && angle <= 90.15);
            boolean isHorizontal = (angle <= 0.15) || (angle >= 179.85);

            if (isVertical && isLineOnGrid(x1, y1, x2, y2, mask)) vertical++;
            else if (isHorizontal && isLineOnGrid(x1, y1, x2, y2, mask)) horizontal++;
        }
        return new int[]{horizontal, vertical};
    }

    // Pozostałe metody bez zmian (jak w oryginalnym kodzie)
    private static Mat bufferedImageToMat(BufferedImage bi) {
        BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = converted.createGraphics();
        graphics.drawImage(bi, 0, 0, null);
        graphics.dispose();
        byte[] data = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(converted.getHeight(), converted.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols(), height = mat.rows();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return image;
    }

    private static Point intersection(double[] line1, double[] line2) {
        double rho1 = line1[0], theta1 = line1[1];
        double rho2 = line2[0], theta2 = line2[1];
        double det = Math.cos(theta1) * Math.sin(theta2) - Math.sin(theta1) * Math.cos(theta2);
        if (Math.abs(det) < 1e-9)
            return null;

        double x = (Math.sin(theta2) * rho1 - Math.sin(theta1) * rho2) / det;
        double y = (Math.cos(theta1) * rho2 - Math.cos(theta2) * rho1) / det;
        return new Point(x, y);
    }

    private static void drawHoughLine(Mat image, double[] line, Scalar color) {
        double rho = line[0];
        double theta = line[1];
        double a = Math.cos(theta);
        double b = Math.sin(theta);
        double x0 = a * rho;
        double y0 = b * rho;
        Point pt1 = new Point(x0 + 1000 * (-b), y0 + 1000 * a);
        Point pt2 = new Point(x0 - 1000 * (-b), y0 - 1000 * a);
        Imgproc.line(image, pt1, pt2, color, 2);
    }

    private static boolean isLineOnGrid(double x1, double y1, double x2, double y2, Mat mask) {
        int totalPixels = 0;
        int matchedPixels = 0;
        List<Point> points = bresenhamLine((int)x1, (int)y1, (int)x2, (int)y2);
        int skip = points.size()/20;

        for(int i = skip; i < points.size()-skip; i++) {
            Point p = points.get(i);
            int x = (int)p.x;
            int y = (int)p.y;

            if(x >= 0 && x < mask.cols() && y >= 0 && y < mask.rows()) {
                totalPixels++;
                if(mask.get(y, x)[0] > 200) matchedPixels++;
            }
        }
        //System.out.println("Matched pixels: " + matchedPixels + " / " + totalPixels);
        return totalPixels > 0 && (matchedPixels/(double)totalPixels) >= 0.05;
    }

    private static List<Point> bresenhamLine(int x0, int y0, int x1, int y1) {
        List<Point> points = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            points.add(new Point(x0, y0));
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
        return points;
    }
}