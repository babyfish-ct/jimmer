package org.babyfish.jimmer.example.cloud.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class RegistryCenterApp {

    public static void main(String[] args) {
        SpringApplication.run(RegistryCenterApp.class, args);
    }
}
