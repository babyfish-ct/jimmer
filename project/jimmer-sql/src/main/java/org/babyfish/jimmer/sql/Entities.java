package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.ArrayList;
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

    @Nullable
    <E> E findById(Class<E> type, Object id);

    @NotNull
    <E> List<E> findByIds(Class<E> type, Iterable<?> ids);

    @NotNull
    <ID, E> Map<ID, E> findMapByIds(Class<E> type, Iterable<ID> ids);

    @Nullable
    <E> E findById(Fetcher<E> fetcher, Object id);

    @NotNull
    <E> List<E> findByIds(Fetcher<E> fetcher, Iterable<?> ids);

    @NotNull
    <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Iterable<ID> ids);

    <E> List<E> findAll(Class<E> type);

    <E> List<E> findAll(Class<E> type, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E, V extends View<E>> List<V> findExample(Class<V> viewType, Example<E> example, TypedProp.Scalar<?, ?> ... sortedProps);

    default <E> SimpleSaveResult<E> save(E entity) {
        return saveCommand(entity).execute();
    }
    
    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities) {
        return saveEntitiesCommand(entities).execute();
    }

    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return save(input.toEntity());
    }

    default <E> SimpleEntitySaveCommand<E> saveCommand(Input<E> input) {
        return saveCommand(input.toEntity());
    }

    <E> BatchEntitySaveCommand<E> saveEntitiesCommand(Iterable<E> entities);

    default <E> BatchEntitySaveCommand<E> saveInputsCommand(Iterable<Input<E>> inputs) {
        List<E> entities = inputs instanceof Collection<?> ?
                new ArrayList<>(((Collection<?>)inputs).size()) :
                new ArrayList<>();
        for (Input<E> input : inputs) {
            entities.add(input.toEntity());
        }
        return saveEntitiesCommand(entities);
    }

    default DeleteResult delete(Class<?> type, Object id) {
        return deleteCommand(type, id).execute();
    }

    default DeleteResult delete(Class<?> type, Object id, DeleteMode mode) {
        return deleteCommand(type, id).setMode(mode).execute();
    }

    DeleteCommand deleteCommand(Class<?> type, Object id);

    default DeleteCommand deleteCommand(Class<?> type, Object id, DeleteMode mode) {
        return deleteCommand(type, id).setMode(mode);
    }

    default DeleteResult deleteAll(Class<?> type, Iterable<?> ids) {
        return deleteAllCommand(type, ids).execute();
    }

    default DeleteResult deleteAll(Class<?> type, Iterable<?> ids, DeleteMode mode) {
        return deleteAllCommand(type, ids).setMode(mode).execute();
    }

    DeleteCommand deleteAllCommand(Class<?> type, Iterable<?> ids);
}
