package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.exception.ExecutionException;

import java.sql.Connection;
import java.sql.SQLException;

class Transactions {

    static void required(Connection con) {
        try {
            if (con.getAutoCommit()) {
                throw new ExecutionException(
                        "The mutation operation must be executed " +
                                "based on JDBC connection without auto commit, " +
                                "Do you forget to open transaction?"
                );
            }
        } catch (SQLException ex) {
            throw new ExecutionException("JDBC Connection is broken", ex);
        }
    }
}
