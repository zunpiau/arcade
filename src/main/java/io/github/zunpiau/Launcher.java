package io.github.zunpiau;

import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Launcher {

    public static void main(String[] args) {
        Map<String, MiniGame> games = Map.of("a", new GiftFactory(), "b", new DessertRush());
        if (args.length > 0) {
            games.values().stream().filter(e -> e.name().equals(args[0]))
                    .findFirst().orElse(games.get(args[0]))
                    .run(Arrays.copyOfRange(args, 1, args.length));
        }
        Scanner scanner = new Scanner(System.in);
        String prompt = games.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> "输入'%s'选择 %s 小游戏".formatted(e.getKey(), e.getValue().name()))
                .collect(Collectors.joining("\n或", "请", ""));
        MiniGame miniGame;
        //noinspection ConditionalBreakInInfiniteLoop
        while (true) {
            System.out.println(prompt);
            miniGame = games.get(scanner.nextLine());
            if (miniGame != null) {
                break;
            }
        }
        System.out.printf("%s 支持以下参数设置：%n", miniGame.name());
        miniGame.paramPrompt().forEach(System.out::println);
        System.out.println("请输入程序参数，空格分隔多个参数。留空并回车以使用默认值");

        String input = scanner.nextLine();
        scanner.close();
        String cmdline = ProcessHandle.current().info().commandLine().orElse("java -jar arcade.jar");
        System.out.printf("下次可以通过参数启动：%s %s %s%n", cmdline, miniGame.name(), input);
        miniGame.run(input.split(" "));
    }

}
