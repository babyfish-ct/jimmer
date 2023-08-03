package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * To be absolutely cache friendly,
 * all query methods like "find...ById(s)" of this class ignore the global filters.
 *
 * The mentions here ignore global filters, only for aggregate root objects,
 * excluding deeper objects fetched by object fetcher.
 */
public interface Entities {

    @NewChain
    Entities forUpdate();

    @NewChain
    Entities forConnection(Connection con);

    <E> E findById(Class<E> type, Object id);

    <E> List<E> findByIds(Class<E> type, Collection<?> ids);

    <ID, E> Map<ID, E> findMapByIds(Class<E> type, Collection<ID> ids);

    <E> E findById(Fetcher<E> fetcher, Object id);

    <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids);

    <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Collection<ID> ids);

    <E> List<E> findAll(Class<E> type, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    default <E> SimpleSaveResult<E> save(E entity) {
        return saveCommand(entity).execute();
    }
    
    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    default <E> BatchSaveResult<E> batchSave(Collection<E> entities) {
        return batchSaveCommand(entities).execute();
    }

    <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities);

    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return save(input.toEntity());
    }

    default <E> SimpleEntitySaveCommand<E> saveCommand(Input<E> input) {
        return saveCommand(input.toEntity());
    }

    default DeleteResult delete(Class<?> type, Object id) {
        return deleteCommand(type, id).execute();
    }

    default DeleteResult delete(Class<?> type, Object id, DeleteMode mode) {
        return deleteCommand(type, id).setMode(mode).execute();
    }

    DeleteCommand deleteCommand(Class<?> type, Object id);

    default DeleteResult batchDelete(Class<?> type, Collection<?> ids) {
        return batchDeleteCommand(type, ids).execute();
    }

    default DeleteResult batchDelete(Class<?> type, Collection<?> ids, DeleteMode mode) {
        return batchDeleteCommand(type, ids).setMode(mode).execute();
    }

    DeleteCommand batchDeleteCommand(Class<?> type, Collection<?> ids);
}
