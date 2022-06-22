package org.babyfish.jimmer.sql.ast;

import java.sql.Connection;

public interface Executable<R> {

    /**
     * Executed on a JDBC connection determined by jimmer-sql.
     * @return Execution result
     */
    R execute();

    /**
     * Execute on the specified JDBC connection.
     *
     * @return Execution result
     */
    R execute(Connection con);
}
