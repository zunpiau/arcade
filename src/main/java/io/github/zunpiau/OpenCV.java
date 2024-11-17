package io.github.zunpiau;

import lombok.SneakyThrows;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenCV {

    static {
        loadLib();
    }

    static Mat decode(byte[] bytes, int flags) {
        return Imgcodecs.imdecode(new MatOfByte(bytes), flags);
    }

    static Mat read(String filename, int flags) {
        return Imgcodecs.imread(filename, flags);
    }

    static Core.MinMaxLocResult match(Mat image, Mat templ) {
        int resultCols = image.cols() - templ.cols() + 1;
        int resultRows = image.rows() - templ.rows() + 1;
        Mat result = new Mat(resultRows, resultCols, CvType.CV_32FC1);
        Imgproc.matchTemplate(image, templ, result, Imgproc.TM_CCOEFF_NORMED);
        return Core.minMaxLoc(result);
    }

    @SneakyThrows
    private static void loadLib() {
        String opencv = System.getProperty("arcade.opencv");
        if (opencv != null && !opencv.isEmpty()) {
            System.load(opencv);
            return;
        }
        String protocol = OpenCV.class.getResource("/" + OpenCV.class.getName().replace('.', '/') + ".class").getProtocol();
        if (protocol.equals("file")) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            Path path;
            if (os.contains("linux")) {
                path = Paths.get("lib", "libopencv_java490.so");
            } else if (os.contains("windows")) {
                path = Paths.get("lib", "opencv_java490.dll");
            } else {
                throw new IllegalStateException("Unsupported platform");
            }
            System.load(path.toAbsolutePath().toString());
        }
    }
}
