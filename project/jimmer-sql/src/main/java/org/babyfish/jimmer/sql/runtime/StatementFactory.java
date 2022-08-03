package org.babyfish.jimmer.sql.runtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementFactory {

    PreparedStatement preparedStatement(
            Connection con,
            String sql
    ) throws SQLException;
}
