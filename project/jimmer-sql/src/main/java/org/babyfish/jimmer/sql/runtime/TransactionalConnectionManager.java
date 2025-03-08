package org.babyfish.jimmer.sql.runtime;

import java.sql.Connection;
import java.util.function.Function;

public interface TransactionalConnectionManager extends ConnectionManager {

    default <R> R execute(Function<Connection, R> block) {
        return executeTransaction(Propagation.SUPPORTS, block);
    }

    default <R> R executeTransaction(Function<Connection, R> block) {
        return executeTransaction(Propagation.REQUIRED, block);
    }

    <R> R executeTransaction(Propagation propagation, Function<Connection, R> block);

    enum Propagation {
        REQUIRED,
        REQUIRES_NEW,
        SUPPORTS,
        NOT_SUPPORTED,
        NEVER,
        MANDATORY
    }
}
