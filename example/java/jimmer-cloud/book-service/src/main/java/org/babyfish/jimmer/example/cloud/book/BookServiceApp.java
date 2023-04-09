package org.babyfish.jimmer.example.cloud.book;

import org.babyfish.jimmer.spring.cloud.MicroServiceExporterAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class BookServiceApp {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(BookServiceApp.class, args);
        Map<?, ?> beans = ctx.getBeansWithAnnotation(Controller.class);
        System.out.println(beans);
    }
}
