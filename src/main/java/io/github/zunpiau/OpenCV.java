package io.github.zunpiau;

import lombok.SneakyThrows;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
        String protocol = OpenCV.class.getResource("/" + OpenCV.class.getName().replace('.', '/') + ".class").getProtocol();
        if (protocol.equals("file")) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            String path;
            String suffix;
            if (os.contains("linux")) {
                path = "/nu/pattern/opencv/linux/x86_64/libopencv_java490.so";
                suffix = ".so";
            } else if (os.contains("windows")) {
                path = "/nu/pattern/opencv/windows/x86_64/opencv_java490.dll";
                suffix = ".dll";
            } else {
                throw new IllegalStateException("Unsupported platform");
            }
            InputStream is = OpenCV.class.getResourceAsStream(path);
            assert is != null;
            File file = File.createTempFile("arcade", suffix);
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            is.close();
            System.load(file.getAbsolutePath());
            file.deleteOnExit();
        }
    }
}
