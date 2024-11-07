package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.datasource.DataSources;
import org.babyfish.jimmer.spring.datasource.TxCallback;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * @author ForteScarlet
 */
public class JimmerLanguageTestHelper {
    public static class DataSourceConfig {
        @Bean
        public DataSource dataSource() {
            return DataSources.create(
                    new TxCallback() {
                        @Override
                        public void open() {
                        }

                        @Override
                        public void commit() {
                        }

                        @Override
                        public void rollback() {
                        }
                    }
            );
        }
    }

    public static void javaLanguage(Object sqlClient) {
        Assertions.assertNotNull(sqlClient);
        Assertions.assertInstanceOf(JSqlClient.class, sqlClient);
    }

    public static void kotlinLanguage(Object sqlClient) {
        Assertions.assertNotNull(sqlClient);
        Assertions.assertInstanceOf(KSqlClient.class, sqlClient);
    }

}
