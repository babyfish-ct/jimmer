package org.babyfish.jimmer.sql.example.database;

import org.babyfish.jimmer.sql.example.Context;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;

import java.sql.Connection;
import java.util.function.Function;

public class ConnectionManagerImpl implements ConnectionManager, Context {

    @Override
    public <R> R execute(Function<Connection, R> block) {
        return block.apply(TRANSACTION_MANAGER.currentConnection());
    }
}
