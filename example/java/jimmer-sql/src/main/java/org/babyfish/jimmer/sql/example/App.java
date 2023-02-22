package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJimmerRepositories("org.babyfish.jimmer.sql.example.dal")
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
