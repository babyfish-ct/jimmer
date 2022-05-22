package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveCommand;

import java.sql.Connection;
import java.util.*;

public class EntitiesImpl implements Entities {

    private SqlClient sqlClient;

    public EntitiesImpl(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public <E> SimpleSaveCommand<E> saveCommand(E entity) {
        return new SimpleSaveCommandImpl<>(sqlClient, entity);
    }

    @Override
    public <E> BatchSaveCommand<E> batchSaveCommand(Collection<E> entities) {
        return new BatchSaveCommandImpl<>(sqlClient, entities);
    }

    @Override
    public DeleteCommand deleteCommand(
            Class<?> entityType,
            Object id
    ) {
        if (id instanceof Collection<?>) {
            throw new IllegalArgumentException("id is collection, do you want to call batchDeleteCommand?");
        }
        return batchDeleteCommand(entityType, Collections.singleton(id));
    }

    @Override
    public DeleteCommand batchDeleteCommand(
            Class<?> entityType,
            Collection<?> ids
    ) {
        ImmutableType immutableType = ImmutableType.get(entityType);
        return new DeleteCommandImpl(sqlClient, immutableType, ids);
    }

    @Override
    public DeleteCommand batchDeleteCommand(
            Class<?> entityType,
            Object... ids
    ) {
        return batchDeleteCommand(entityType, Arrays.asList(ids));
    }
}
