package org.babyfish.jimmer.sql.example.cfg;

import org.babyfish.jimmer.spring.cfg.JimmerCustomizer;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

/*
 * Initialize H2 in-memory database if the application is started by default profile.
 *
 * This class must implement `JimmerCustomizer`(before the sql client is created),
 * not JimmerInitializer(after the sql client is created),
 * because `jimmer.database-validation-mode` in `application.yml` validates database
 * before the sql client is created.
 */
@Component
public class H2Initializer implements JimmerCustomizer {

    private final DataSource dataSource;

    private final String url;

    public H2Initializer(
            DataSource dataSource,
            @Value("${spring.datasource.url}") String url) {
        this.dataSource = dataSource;
        this.url = url;
    }

    @Override
    public void customize(JSqlClient.Builder builder) {
        if (url.startsWith("jdbc:h2:")) {
            initH2();
        }
    }

    private void initH2() {
        try (Connection con = dataSource.getConnection()) {
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
            }
        } catch (IOException | SQLException ex) {
            throw new RuntimeException("Cannot initialize h2 database", ex);
        }
    }
}
