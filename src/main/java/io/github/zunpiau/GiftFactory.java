package io.github.zunpiau;

import lombok.SneakyThrows;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GiftFactory {

    private static final Path DEBUG_DIR = Paths.get("giftFactory", "debug");
    private static final boolean DEBUG = Boolean.getBoolean("arcade.debug");

    private static int COUNTER = 0;

    public static void main(String[] args) throws InterruptedException {
        int ROUND_MS = Util.parseArg(args, 0, 100, 500, 250);
        int BURST_MS = 5000 - ROUND_MS / 2;
        int BURST_INTERVAL = Util.parseArg(args, 1, 1, 12, 2);

        Map<Type, List<ImgTemplate>> allTemplates = setupTemplate();
        List<ImgTemplate> startImgTemps = allTemplates.remove(Type.start);
        List<ImgTemplate> templates = allTemplates.values().stream().flatMap(List::stream).toList();
        Env env = Env.getInstance();
        if (DEBUG) {
            Util.setupDebugDir(DEBUG_DIR, Type.class);
        }

        System.out.println("正在检测游戏开始...");
        int i = 0, maxDetect = 200;
        while (++i < maxDetect) {
            byte[] capture = env.capture(0, 0, 145, 85);
            Mat mat = OpenCV.decode(capture, Imgcodecs.IMREAD_GRAYSCALE);
            MatchResult result = match(mat, startImgTemps);
            writeDebug(Type.start, result.template, result.val, capture);
            if (result.match) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(ROUND_MS);
        }
        if (i == maxDetect) {
            System.out.println("检测超时，自动退出");
            System.exit(1);
        }

        System.out.println("游戏开始");
        long lastRound, lastBurst = 0;
        boolean paused = false;
        //noinspection InfiniteLoopStatement
        while (true) {
            lastRound = System.currentTimeMillis();
            byte[] capture = env.capture(800, 520, 320, 320);
            Mat mat = OpenCV.decode(capture, Imgcodecs.IMREAD_GRAYSCALE);
            MatchResult result = match(mat, templates);
            Type type = result.match ? result.template.type : Type.gift;
            writeDebug(type, result.template, result.val, capture);

            if (type != Type.pause && paused) {
                System.out.println("游戏恢复");
            } else if (type == Type.pause && !paused) {
                System.out.println("游戏暂停");
            }
            paused = type == Type.pause;
            switch (type) {
                case gold -> {
                    if (lastRound - lastBurst < 5000) {
                        env.press("A");
                    } else {
                        System.out.println("BURST 开始");
                        do {
                            env.press("A");
                            TimeUnit.MILLISECONDS.sleep(BURST_INTERVAL);
                        } while (System.currentTimeMillis() - lastRound <= BURST_MS);
                        lastBurst = System.currentTimeMillis();
                        System.out.println("BURST 结束");
                        continue;
                    }
                }
                case end -> {
                    int timeout = lastBurst == 0 ? 1 : 5;
                    System.out.printf("回合结束，将在%d秒后进入下一回合。按下 Ctrl+C 或者关闭窗口以结束脚本\n", timeout);
                    TimeUnit.SECONDS.sleep(timeout);
                    env.click(1052, 714);
                    System.out.println("进入下一回合");
                    TimeUnit.SECONDS.sleep(3);
                    continue;
                }
                case gift -> env.press("A");
                case rapture -> env.press("D");
            }

            long remain = ROUND_MS - System.currentTimeMillis() + lastRound;
            if (remain > 0) {
                TimeUnit.MILLISECONDS.sleep(remain);
            }
        }
    }

    private static Map<Type, List<ImgTemplate>> setupTemplate() {
        Map<Type, List<ImgTemplate>> res = new HashMap<>();
        Util.listDir(Paths.get("giftFactory", "template")).forEach(d -> {
            if (!Files.isDirectory(d)) {
                return;
            }
            Type type = Type.valueOf(d.getFileName().toString());
            List<ImgTemplate> templates = Util.listDir(d)
                    .map(f -> new ImgTemplate(f.getFileName().toString(), OpenCV.read(f.toString(), Imgcodecs.IMREAD_GRAYSCALE), type))
                    .toList();
            res.put(type, templates);
        });
        return res;
    }


    @SneakyThrows
    private static MatchResult match(Mat mat, List<ImgTemplate> templates) {
        double maxVal = Double.NEGATIVE_INFINITY;
        ImgTemplate maxTemplate = null;
        for (ImgTemplate template : templates) {
            double val = OpenCV.match(mat, template.img, null).maxVal;
            if (val > template.type.threshold) {
                return new MatchResult(true, template, val);
            }
            if (val > maxVal) {
                maxTemplate = template;
                maxVal = val;
            }
        }
        return new MatchResult(false, maxTemplate, maxVal);
    }

    @SneakyThrows
    private static void writeDebug(Type type, ImgTemplate template, double val, byte[] capture) {
        if (!DEBUG || template == null) {
            return;
        }
        String filename = "%05d_%s_%s_%.3f.bmp".formatted(COUNTER++, template.type.name(), template.filename, val);
        Files.write(DEBUG_DIR.resolve(type.name()).resolve(filename), capture);
    }

    private record ImgTemplate(String filename, Mat img, Type type) {

    }

    private record MatchResult(boolean match, ImgTemplate template, double val) {

    }

    private enum Type {

        end(0.75),
        gold(0.75),
        rapture(0.5),
        gift(1),
        start(0.8),
        pause(0.9),
        ;
        private final double threshold;

        Type(double threshold) {
            this.threshold = threshold;
        }
    }

}
