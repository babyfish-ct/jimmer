package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;

import java.util.Collection;

public interface Entities {

    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    <E> BatchEntitySaveCommand<E> batchSaveCommand(
            Collection<E> entities
    );

    DeleteCommand deleteCommand(
            Class<?> entityType,
            Object id
    );

    DeleteCommand batchDeleteCommand(
            Class<?> entityType,
            Collection<?> ids
    );

    DeleteCommand batchDeleteCommand(
            Class<?> entityType,
            Object ... ids
    );
}
