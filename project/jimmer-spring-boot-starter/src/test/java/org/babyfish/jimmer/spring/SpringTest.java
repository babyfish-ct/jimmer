package org.babyfish.jimmer.spring;

import org.babyfish.jimmer.spring.dal.BookRepository;
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.babyfish.jimmer.sql.JSqlClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableJimmerRepositories(basePackageClasses = BookRepository.class)
public class SpringTest {

    @Configuration
    static class SqlClientConfig {

        @Bean
        public JSqlClient sqlClient() {
            return JSqlClient.newBuilder().build();
        }
    }

    @Autowired
    private BookRepository bookRepository;

    @Test
    public void test() {
        // bookRepository.findAll();
    }
}
