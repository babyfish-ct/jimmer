package org.babyfish.jimmer.spring;

import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

public class AbstractTest {

    protected static void initDatabase(DataSource dataSource) {

        InputStream stream = AbstractTest.class.getClassLoader().getResourceAsStream("database.sql");
        if (stream == null) {
            Assertions.fail("Failed to initialize database, cannot load 'database.sql'");
        }

        try (Reader reader = new InputStreamReader(stream)) {
            StringBuilder builder = new StringBuilder();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
            try (Connection con = dataSource.getConnection()) {
                con.createStatement().execute(builder.toString());
            }
        } catch (IOException | SQLException ex) {
            Assertions.fail("Failed to initialize database", ex);
        }
    }
}
