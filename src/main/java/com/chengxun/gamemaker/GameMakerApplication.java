package com.chengxun.gamemaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GameMakerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameMakerApplication.class, args);
    }
}
