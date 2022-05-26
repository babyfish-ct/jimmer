package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;

import java.util.*;

public class EntitiesImpl implements Entities {

    private SqlClient sqlClient;

    public EntitiesImpl(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public <E> SimpleEntitySaveCommand<E> saveCommand(E entity) {
        return new SimpleEntitySaveCommandImpl<>(sqlClient, entity);
    }

    @Override
    public <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities) {
        return new BatchEntitySaveCommandImpl<>(sqlClient, entities);
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
}