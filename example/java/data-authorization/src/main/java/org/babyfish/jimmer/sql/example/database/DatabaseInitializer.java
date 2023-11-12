package org.babyfish.jimmer.sql.example.database;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.example.Context;
import org.babyfish.jimmer.sql.runtime.Initializer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer implements Initializer, Context {

    @Override
    public void initialize(JSqlClient sqlClient) throws Exception {
        TRANSACTION_MANAGER.execute(() -> {
            try {
                String text = loadSql();
                initDatabase(text);
            } catch (IOException | SQLException ex) {
                throw new RuntimeException("Cannot initialize h2 database", ex);
            }
            return null;
        });
    }

    private String loadSql() throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buf = new char[1024];
        try (Reader reader = new InputStreamReader(DatabaseInitializer.class.getClassLoader().getResourceAsStream("database.sql"))) {
            int len;
            while ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
        }
        return builder.toString();
    }

    private void initDatabase(String text) throws SQLException {
        String[] arr = text.split("\\s*;\\s*");
        for (String sql : arr) {
            Statement statement = TRANSACTION_MANAGER.currentConnection().createStatement();
            statement.executeUpdate(sql);
        }
    }
}
