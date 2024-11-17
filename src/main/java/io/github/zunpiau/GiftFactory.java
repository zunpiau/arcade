package io.github.zunpiau;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GiftFactory {

    private static final Path DEBUG_DIR = Paths.get("giftFactory", "debug");
    private static final boolean DEBUG = false;
    private static final int ROUND_MS = 250;
    private static final int BURST_MS = 5000 - ROUND_MS * 2;


    public static void main(String[] args) throws IOException, InterruptedException {
        cleanupDebugDir();
        List<ImgTemplate> templates = setupTemplate();
        Env env = Env.getInstance();

        long lastRound, lastBurst = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            lastRound = System.currentTimeMillis();
            byte[] capture = env.capture(830, 530, 320, 260);
            Mat mat = OpenCV.decode(capture, Imgcodecs.IMREAD_GRAYSCALE);
            Type type = match(mat, capture, templates, lastRound);
            switch (type) {
                case gold -> {
                    if (lastRound - lastBurst < 5000) {
                        System.out.println("BURST ignore");
                        env.press('A');
                    } else {
                        do {
                            env.press('A');
                            TimeUnit.MILLISECONDS.sleep(1);
                        } while (System.currentTimeMillis() - lastRound <= BURST_MS);
                        lastBurst = System.currentTimeMillis();
                        continue;
                    }
                }
                case end -> {
                    System.out.println("Waiting...");
                    TimeUnit.MILLISECONDS.sleep(5000);
                    env.click(1052, 714);
                    TimeUnit.MILLISECONDS.sleep(3200);
                    continue;
                }
                case gift -> env.press('A');
                case rapture -> env.press('D');
            }
            long remain = ROUND_MS - System.currentTimeMillis() + lastRound;
            if (remain > 0) {
                TimeUnit.MILLISECONDS.sleep(remain);
            }
        }
    }

    @SuppressWarnings("resource")
    private static List<ImgTemplate> setupTemplate() throws IOException {
        List<ImgTemplate> templates = new ArrayList<>();
        Files.list(Paths.get("giftFactory", "template")).forEach(d -> {
            try {
                Type type = Type.valueOf(d.getFileName().toString());
                Files.list(d).forEach(f -> templates.add(new ImgTemplate(f.getFileName().toString(),
                        OpenCV.read(f.toString(), Imgcodecs.IMREAD_GRAYSCALE), type)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return templates;
    }

    @SuppressWarnings("resource")
    private static void cleanupDebugDir() throws IOException {
        if (!DEBUG) {
            return;
        }
        Files.list(DEBUG_DIR).forEach(d -> {
            try {
                Files.list(d).forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Type match(Mat mat, byte[] capture, List<ImgTemplate> templates, long now) throws IOException {
        double maxVal = 0;
        String maxFilename = "";
        for (ImgTemplate template : templates) {
            double val = OpenCV.match(mat, template.img).maxVal;
            if (val > template.type.threshold) {
                if (DEBUG) {
                    Files.write(DEBUG_DIR.resolve(template.type.name())
                            .resolve(now + "_" + template.filename + "_" + val + ".bmp"), capture);
                }
                return template.type;
            }
            if (val > maxVal) {
                maxFilename = template.filename;
                maxVal = val;
            }
        }
        if (DEBUG) {
            Files.write(DEBUG_DIR.resolve(Type.gift.name())
                    .resolve(now + "_" + maxFilename + "_" + maxVal + ".bmp"), capture);
        }
        return Type.gift;
    }


    private record ImgTemplate(String filename, Mat img, Type type) {

    }

    private enum Type {

        end(0.75),
        gold(0.75),
        rapture(0.48),
        gift(1),
        ;
        private final double threshold;

        Type(double threshold) {
            this.threshold = threshold;
        }
    }

}
