package org.babyfish.jimmer.sql.runtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Executor {

    <R> R execute(
            Connection con,
            String sql,
            List<Object> variables,
            SqlFunction<PreparedStatement, R> block
    );
}
