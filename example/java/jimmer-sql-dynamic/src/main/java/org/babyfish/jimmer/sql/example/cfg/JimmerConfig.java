package org.babyfish.jimmer.sql.example.cfg;

import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.example.model.JimmerModule;
import org.babyfish.jimmer.sql.runtime.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class JimmerConfig {

    /*
     * 1. Jimmer requires this entity manager,
     *    if you do not want to directly create JSqlClient/KSqlClient
     *
     * 2. Kotlin requires `jimmer.language=kotlin` in `application.yml`
     */
    @Bean
    public EntityManager entityManager() {
        return JimmerModule.ENTITY_MANAGER;
    }

    /*
     * This bean is used to determine dialect at runtime.
     *
     * If this runtime determination is unnecessary,
     * please remove this bean and directly configure
     * `jimmer.dialect` in `application.yml` or `application.properties`
     */
    @Bean
    public Dialect dialect(
            @Value("${spring.datasource.url}") String jdbcUrl,
            DataSource dataSource
    ) throws SQLException {
        boolean isH2 = jdbcUrl.startsWith("jdbc:h2:");
        if (isH2) {
            initializeH2Database(dataSource);
            return new H2Dialect();
        } else {
            return new MySqlDialect();
        }
    }

    private void initializeH2Database(DataSource dataSource) throws SQLException {
        Connection con = dataSource.getConnection();
        try {
            InputStream inputStream = JimmerConfig.class
                    .getClassLoader()
                    .getResourceAsStream("h2-database.sql");
            if (inputStream == null) {
                throw new RuntimeException("no `h2-database.sql`");
            }
            try (Reader reader = new InputStreamReader(inputStream)) {
                char[] buf = new char[1024];
                StringBuilder builder = new StringBuilder();
                while (true) {
                    int len = reader.read(buf);
                    if (len == -1) {
                        break;
                    }
                    builder.append(buf, 0, len);
                }
                con.createStatement().execute(builder.toString());
            } catch (IOException | SQLException ex) {
                throw new RuntimeException("Cannot initialize h2 database", ex);
            }
        } finally {
            con.close();
        }
    }
}
