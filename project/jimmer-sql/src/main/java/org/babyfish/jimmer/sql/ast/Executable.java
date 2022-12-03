package org.babyfish.jimmer.sql.ast;

import java.sql.Connection;

public interface Executable<R> {

    /**
     * Executed on a JDBC connection determined by jimmer-sql.
     * @return Execution result
     */
    default R execute() {
        return execute(null);
    }

    /**
     * Execute on the specified JDBC connection.
     *
     * @return Execution result
     */
    R execute(Connection con);
}
