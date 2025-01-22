package io.github.zunpiau;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Launcher {

    public static void main(String[] args) throws Throwable {
        Map<String, String> names = Map.of("a", "GiftFactory", "b", "DessertRush");
        if (args.length > 0) {
            launch(names.getOrDefault(args[0], args[0]), Arrays.copyOfRange(args, 1, args.length));
        }
        Scanner scanner = new Scanner(System.in);
        String name;
        //noinspection ConditionalBreakInInfiniteLoop
        while (true) {
            System.out.println("请输入'a'选择 Gift Factory 小游戏\n或'b'选择 Dessert Rush 小游戏");
            name = names.get(scanner.nextLine());
            if (name != null) {
                break;
            }
        }
        System.out.println("请输入程序参数，空格分隔多个参数。留空以使用默认值");
        String input = scanner.nextLine();
        scanner.close();
        launch(name, input.split(" "));
    }

    private static void launch(String name, String[] args) throws Throwable {
        Class<?> game = Class.forName(Launcher.class.getPackage().getName() + '.' + name);
        MethodHandles.lookup().findStatic(game, "main", MethodType.methodType(void.class, String[].class))
                .invoke((Object) args);
    }

}
