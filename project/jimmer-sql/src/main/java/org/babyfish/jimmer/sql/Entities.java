package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.NewChain;
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

    <E> E findById(Class<E> entityType, Object id);

    <E> List<E> findByIds(Class<E> entityType, Collection<?> ids);

    <ID, E> Map<ID, E> findMapByIds(Class<E> entityType, Collection<ID> ids);

    <E> E findById(Fetcher<E> fetcher, Object id);

    <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids);

    <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Collection<ID> ids);

    <E> List<E> findAll(Class<E> type, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> SimpleSaveResult<E> save(E entity);
    
    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    <E> BatchSaveResult<E> batchSave(Collection<E> entities);

    <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities);

    DeleteResult delete(Class<?> entityType, Object id);

    DeleteCommand deleteCommand(Class<?> entityType, Object id);

    DeleteResult batchDelete(Class<?> entityType, Collection<?> ids);

    DeleteCommand batchDeleteCommand(Class<?> entityType, Collection<?> ids);
}
