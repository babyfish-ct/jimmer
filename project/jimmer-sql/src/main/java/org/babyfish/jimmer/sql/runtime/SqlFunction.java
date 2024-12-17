package org.babyfish.jimmer.sql.runtime;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T, R> {

    R apply(T value, Executor.Args<R> args) throws SQLException;
}
