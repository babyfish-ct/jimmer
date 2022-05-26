package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.mutation.*;

import java.util.Collection;

public interface Entities {

    default <E> SimpleSaveResult<E> save(E entity) {
        return saveCommand(entity).execute();
    }

    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    default <E> BatchSaveResult<E> batchSave(Collection<E> entities) {
        return batchSaveCommand(entities).execute();
    }

    <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities);

    default DeleteResult delete(Class<?> entityType, Object id) {
        return deleteCommand(entityType, id).execute();
    }

    DeleteCommand deleteCommand(Class<?> entityType, Object id);

    default DeleteResult batchDelete(Class<?> entityType, Collection<?> ids) {
        return batchDeleteCommand(entityType, ids).execute();
    }

    DeleteCommand batchDeleteCommand(Class<?> entityType, Collection<?> ids);
}
