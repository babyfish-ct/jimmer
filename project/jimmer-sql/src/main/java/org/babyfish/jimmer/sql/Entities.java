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

    default <E> SimpleSaveResult<E> save(E entity) {
        return saveCommand(entity).execute();
    }

    default <E> SimpleSaveResult<E> save(E entity, boolean autoAttachingAll) {
        return saveCommand(entity, autoAttachingAll).execute();
    }
    
    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    default <E> SimpleEntitySaveCommand<E> saveCommand(E entity, boolean autoAttachAll) {
        SimpleEntitySaveCommand<E> command = saveCommand(entity);
        if (!autoAttachAll) {
            return command;
        }
        return command.configure(AbstractEntitySaveCommand.Cfg::setAutoAttachingAll);
    }

    default <E> BatchSaveResult<E> batchSave(Collection<E> entities) {
        return batchSaveCommand(entities).execute();
    }

    default <E> BatchSaveResult<E> batchSave(Collection<E> entities, boolean autoAttachingAll) {
        return batchSaveCommand(entities, autoAttachingAll).execute();
    }

    <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities);

    default <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities, boolean autoAttachAll) {
        BatchEntitySaveCommand<E> command = batchSaveCommand(entities);
        if (!autoAttachAll) {
            return command;
        }
        return command.configure(AbstractEntitySaveCommand.Cfg::setAutoAttachingAll);
    }

    default DeleteResult delete(Class<?> entityType, Object id) {
        return deleteCommand(entityType, id).execute();
    }

    DeleteCommand deleteCommand(Class<?> entityType, Object id);

    default DeleteResult batchDelete(Class<?> entityType, Collection<?> ids) {
        return batchDeleteCommand(entityType, ids).execute();
    }

    DeleteCommand batchDeleteCommand(Class<?> entityType, Collection<?> ids);
}
