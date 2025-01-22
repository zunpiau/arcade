package io.github.zunpiau;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    @SneakyThrows
    static Stream<Path> listDir(Path d) {
        return Files.list(d);
    }

    @SneakyThrows
    static <T extends Enum<T>> void setupDebugDir(Path debugDir, Class<T> e) {
        Set<String> set = Arrays.stream(e.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
        if (!Files.exists(debugDir)) {
            Files.createDirectories(debugDir);
        } else {
            Util.listDir(debugDir).filter(Files::isDirectory).forEach(p -> {
                Util.cleanDir(p);
                set.remove(p.getFileName().toString());
            });
        }
        set.forEach(t -> Util.createDir(debugDir.resolve(t)));
    }

    static int parseArg(String[] args, int idx, int min, int max, int defaultVal) {
        if (args.length >= idx + 1) {
            try {
                int i = Integer.parseInt(args[idx]);
                return Math.max(Math.min(i, max), min);
            } catch (NumberFormatException ignore) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    private static void cleanDir(Path p) {
        listDir(p).forEach(Util::deleteFile);
    }

    @SneakyThrows
    private static void deleteFile(Path f) {
        Files.delete(f);
    }

    @SneakyThrows
    private static void createDir(Path dir) {
        Files.createDirectories(dir);
    }
}
