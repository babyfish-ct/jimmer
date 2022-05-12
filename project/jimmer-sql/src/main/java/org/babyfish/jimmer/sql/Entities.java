package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveCommand;

import java.util.Collection;

public interface Entities {

    <E> SimpleSaveCommand<E> saveCommand(E entity);

    <E> BatchSaveCommand<E> batchSaveCommand(
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
