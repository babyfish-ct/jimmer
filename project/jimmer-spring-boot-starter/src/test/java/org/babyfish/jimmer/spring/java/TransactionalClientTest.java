package org.babyfish.jimmer.spring.java;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.spring.SqlClients;
import org.babyfish.jimmer.spring.cfg.ErrorTranslatorConfig;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.babyfish.jimmer.spring.cfg.SqlClientConfig;
import org.babyfish.jimmer.spring.datasource.DataSources;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.java.model.BookFetcher;
import org.babyfish.jimmer.spring.java.model.BookTable;
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.babyfish.jimmer.spring.transaction.JimmerTransactionManager;
import org.babyfish.jimmer.spring.transaction.TransactionalSqlClients;
import org.babyfish.jimmer.sql.JSqlClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@SpringBootTest()
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableConfigurationProperties(JimmerProperties.class)
public class TransactionalClientTest {

    @Autowired
    private JSqlClient sqlClient;

    @Transactional("tm")
    @Test
    public void test() {
        BookTable table = BookTable.$;
        List<Book> books = sqlClient
                .createQuery(table)
                .where(table.name().eq("GraphQL in Action"))
                .select(
                        table.fetch(
                                BookFetcher.$
                                        .name()
                                        .edition()
                        )
                )
                .execute();
        Assertions.assertEquals(
                "[" +
                        "{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\",\"name\":\"GraphQL in Action\",\"edition\":1}, " +
                        "{\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\",\"name\":\"GraphQL in Action\",\"edition\":2}, " +
                        "{\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\",\"name\":\"GraphQL in Action\",\"edition\":3}" +
                        "]",
                books.toString()
        );
    }

    @Configuration
    public static class Config {

        @Bean
        public DataSource dataSource() {
            return DataSources.create(null);
        }

        @Bean
        public JimmerTransactionManager tm(ApplicationContext ctx) {
            return new JimmerTransactionManager(SqlClients.java(ctx, dataSource()));
        }

        @Bean
        public JSqlClient sqlClient() {
            return TransactionalSqlClients.java();
        }
    }
}
