package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Entities {

    <E> E findById(Class<E> entityType, Object id);

    <E> List<E> findByIds(Class<E> entityType, Collection<Object> ids);

    <ID, E> Map<ID, E> findMapByIds(Class<E> entityType, Collection<ID> ids);

    <E> E findById(Fetcher<E> fetcher, Object id);

    <E> List<E> findByIds(Fetcher<E> fetcher, Collection<Object> ids);

    <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Collection<ID> ids);

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
