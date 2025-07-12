package io.github.zunpiau;

import java.util.List;

public interface MiniGame {

    String name();

    List<String> paramPrompt();

    void run(String[] args);

}
