package com.hou.hacker;

import com.hou.hacker.question.HackerRankerTransactions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HackerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(HackerApplication.class, args);
    }

    @Override
    public void run(String... args) {
            new HackerRankerTransactions().testRun(false);
    }
}
