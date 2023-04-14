package org.babyfish.jimmer.sql.example.cfg;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.Initializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;

/*
 * Initialize H2 in-memory database if the application is started by default profile.
 */
@Component
public class H2Initializer implements Initializer {

    private final DataSource dataSource;

    private final String url;

    public H2Initializer(
            DataSource dataSource,
            @Value("${spring.datasource.url}") String url) {
        this.dataSource = dataSource;
        this.url = url;
    }

    @Override
    public void initialize(@NotNull JSqlClient sqlClient) throws Exception {
        if (url.startsWith("jdbc:h2:")) {
            initH2();
        }
    }

    private void initH2() throws Exception {
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
        }
    }
}
