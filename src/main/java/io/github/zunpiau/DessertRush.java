package io.github.zunpiau;

import lombok.SneakyThrows;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DessertRush {

    private static final Path DEBUG_DIR = Paths.get("dessertRush", "debug");
    private static final boolean DEBUG = Boolean.getBoolean("arcade.debug");

    private static int COUNTER = 0;

    @SneakyThrows
    public static void main(String[] args) {
        int ROUND_MS = Util.parseArg(args,  0, 10, 500, 220);
        int BURST_MS = Util.parseArg(args,  1, 1, 100, 10);
        int BURST_TIMES = Util.parseArg(args,  2, 1, 50, 10);

        Map<Type, List<ImgTemplate>> templates = setupTemplate();
        Env env = Env.getInstance();
        if (DEBUG) {
            Util.setupDebugDir(DEBUG_DIR, Result.class);
        }

        System.out.println("正在检测游戏开始...");
        int i = 0, maxDetect = 200;
        while (++i < maxDetect) {
            byte[] capture = env.capture(650, 0, 100, 100);
            Mat mat = OpenCV.decode(capture, Imgcodecs.IMREAD_GRAYSCALE);
            if (match(mat, templates.get(Type.start)) != null) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(ROUND_MS);
        }
        if (i == maxDetect) {
            System.out.println("检测超时，自动退出");
            System.exit(1);
        }

        System.out.println("游戏开始");
        boolean paused = false, has_burst = false;
        //noinspection InfiniteLoopStatement
        while (true) {
            byte[] capture = env.capture(715, 500, 550, 320);
            Mat mat = OpenCV.decode(capture, Imgcodecs.IMREAD_GRAYSCALE);

            Result result = null;
            boolean burst = match(mat, templates.get(Type.burst)) != null;
            if (burst) {
                result = Result.burst;
            } else {
                Core.MinMaxLocResult dessert = match(mat, templates.get(Type.dessert));
                Core.MinMaxLocResult bomb = match(mat, templates.get(Type.bomb));
                if (dessert != null && bomb != null) {
                    result = Result.both;
                } else if (dessert != null && dessert.maxLoc.x > 120 && dessert.maxLoc.x < 360) {
                    result = Result.dessert;
                } else if (bomb != null && bomb.maxLoc.x > 120 && bomb.maxLoc.x < 360) {
                    result = Result.bomb;
                }
            }
            if (result != null) {
                if (paused) {
                    System.out.println("游戏恢复");
                }
                paused = false;
                env.press(result.key);
                if (!burst && has_burst) {
                    for (int j = 0; j < BURST_TIMES; j++) {
                        TimeUnit.MILLISECONDS.sleep(BURST_MS);
                        env.press(result.key);
                    }
                }
                has_burst = burst;
                TimeUnit.MILLISECONDS.sleep(ROUND_MS);
                writeDebug(result.name(), mat);
                continue;
            }

            boolean pause = match(mat, templates.get(Type.pause)) != null;
            if (pause && !paused) {
                System.out.println("游戏暂停");
            }
            paused = pause;
            if (pause) {
                TimeUnit.MILLISECONDS.sleep(ROUND_MS / 2);
                continue;
            }
            if (match(mat, templates.get(Type.end)) != null) {
                int timeout = 5;
                System.out.printf("回合结束，将在%d秒后进入下一回合。按下 Ctrl+C 或者关闭窗口以结束脚本\n", timeout);
                TimeUnit.SECONDS.sleep(timeout);
                env.click(1052, 714);
                System.out.println("进入下一回合");
                TimeUnit.SECONDS.sleep(3);
                continue;
            }
            writeDebug(Result.none.name(), mat);
        }
    }

    private static Map<Type, List<ImgTemplate>> setupTemplate() {
        Map<Type, List<ImgTemplate>> res = new HashMap<>();
        Util.listDir(Paths.get("dessertRush", "template")).forEach(d -> {
            if (!Files.isDirectory(d)) {
                return;
            }
            Type type = Type.valueOf(d.getFileName().toString());
            List<ImgTemplate> templates = Util.listDir(d)
                    .map(f -> new ImgTemplate(f.getFileName().toString()
                            , OpenCV.read(f.toString(), Imgcodecs.IMREAD_GRAYSCALE)
                            , OpenCV.readMask(f.toString())
                            , type))
                    .toList();
            res.put(type, templates);
        });
        return res;
    }

    @SneakyThrows
    private static void writeDebug(String dir, Mat mat) {
        OpenCV.write(DEBUG_DIR.resolve(dir).resolve("%05d.bmp".formatted(COUNTER++)).toAbsolutePath().toString(), mat);
    }

    @SneakyThrows
    private static Core.MinMaxLocResult match(Mat mat, List<ImgTemplate> templates) {
        for (ImgTemplate template : templates) {
            Core.MinMaxLocResult result = OpenCV.match(mat, template.img, template.mask);
            if (result.maxVal > template.type.threshold) {
                return result;
            }
        }
        return null;
    }

    private record ImgTemplate(String filename, Mat img, Mat mask, Type type) {

    }

    private enum Result {
        none(null),
        both("AD"),
        dessert("A"),
        bomb("D"),
        burst("A"),
        ;
        private final String key;

        Result(String key) {
            this.key = key;
        }
    }

    private enum Type {

        end(0.75),
        bomb(0.8),
        dessert(0.8),
        burst(0.8),
        start(0.9),
        pause(0.9),
        ;
        private final double threshold;

        Type(double threshold) {
            this.threshold = threshold;
        }
    }

}
