package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class EntitiesImpl implements Entities {

    private SqlClient sqlClient;

    public EntitiesImpl(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public Executable<Map<String, Integer>> deleteCommand(
            Class<?> entityType,
            Object id
    ) {
        return batchDeleteCommand(entityType, Collections.singleton(id));
    }

    @Override
    public Executable<Map<String, Integer>> batchDeleteCommand(
            Class<?> entityType,
            Collection<?> ids
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(entityType);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type from '" +
                            entityType.getName() +
                            "'"
            );
        }
        return new DeleteCommand(sqlClient, immutableType, ids);
    }

    @Override
    public Executable<Map<String, Integer>> batchDeleteCommand(
            Class<?> entityType,
            Object... ids
    ) {
        return batchDeleteCommand(entityType, Arrays.asList(ids));
    }

    private static class DeleteCommand implements Executable<Map<String, Integer>> {

        private SqlClient sqlClient;

        private ImmutableType immutableType;

        private Collection<?> ids;

        public DeleteCommand(
                SqlClient sqlClient,
                ImmutableType immutableType,
                Collection<?> ids
        ) {
            this.sqlClient = sqlClient;
            this.immutableType = immutableType;
            this.ids = ids;
        }

        @Override
        public Map<String, Integer> execute(Connection con) {
            Deleter deleter = new Deleter(sqlClient, con);
            deleter.addPreHandleInput(immutableType, ids);
            return deleter.execute();
        }
    }
}
