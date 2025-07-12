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
                .map(e -> I18n.t("launcher_prompt").formatted(e.getKey(), e.getValue().name()))
                .collect(Collectors.joining(I18n.t("launcher_prompt_del"), I18n.t("launcher_prompt_pre"), ""));
        MiniGame miniGame;
        //noinspection ConditionalBreakInInfiniteLoop
        while (true) {
            System.out.println(prompt);
            miniGame = games.get(scanner.nextLine());
            if (miniGame != null) {
                break;
            }
        }
        System.out.printf(I18n.t("launcher_param"), miniGame.name());
        miniGame.paramPrompt().forEach(System.out::println);
        System.out.println(I18n.t("launcher_input"));

        String input = scanner.nextLine();
        scanner.close();
        String cmdline = ProcessHandle.current().info().commandLine().orElse("java -jar arcade.jar");
        System.out.printf(I18n.t("launcher_cmd"), cmdline, miniGame.name(), input);
        miniGame.run(input.split(" "));
    }

}
