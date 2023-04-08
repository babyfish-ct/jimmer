package org.babyfish.jimmer.example.cloud.author.cfg;

import org.babyfish.jimmer.spring.cfg.JimmerCustomizer;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseInitializer implements JimmerCustomizer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void customize(JSqlClient.Builder builder) {
        try (Connection con = dataSource.getConnection()) {
            InputStream inputStream = JimmerConfig.class
                    .getClassLoader()
                    .getResourceAsStream("author.sql");
            if (inputStream == null) {
                throw new RuntimeException("no `author.sql`");
            }
            try (Reader reader = new InputStreamReader(inputStream)) {
                char[] buf = new char[1024];
                StringBuilder sqlBuilder = new StringBuilder();
                while (true) {
                    int len = reader.read(buf);
                    if (len == -1) {
                        break;
                    }
                    sqlBuilder.append(buf, 0, len);
                }
                con.createStatement().execute(sqlBuilder.toString());
            }
        } catch (IOException | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
