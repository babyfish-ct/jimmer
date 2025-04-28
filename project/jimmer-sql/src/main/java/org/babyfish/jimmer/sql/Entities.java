package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * To be absolutely cache friendly,
 * all query methods like "find...ById(s)" of this class ignore the global filters.
 *
 * The mentions here ignore global filters, only for aggregate root objects,
 * excluding deeper objects fetched by object fetcher.
 */
public interface Entities extends DeprecatedSaveOperations {

    @NewChain
    Entities forUpdate();

    @NewChain
    Entities forConnection(Connection con);

    /**
     * @param <T> Entity type or output DTO type
     */
    @Nullable
    <T> T findById(Class<T> type, Object id);

    /**
     * @param <T> Entity type or output DTO type
     */
    @NotNull
    <T> T findOneById(Class<T> type, Object id);

    /**
     * @param <T> Entity type or output DTO type
     */
    @NotNull
    <T> List<T> findByIds(Class<T> type, Iterable<?> ids);

    /**
     * @param <T> Entity type or output DTO type
     */
    @NotNull
    <ID, T> Map<ID, T> findMapByIds(Class<T> type, Iterable<ID> ids);

    @Nullable
    <E> E findById(Fetcher<E> fetcher, Object id);

    @NotNull
    <E> E findOneById(Fetcher<E> fetcher, Object id);

    @NotNull
    <E> List<E> findByIds(Fetcher<E> fetcher, Iterable<?> ids);

    @NotNull
    <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Iterable<ID> ids);

    <T> List<T> findAll(Class<T> type);

    <T> List<T> findAll(Class<T> type, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, TypedProp.Scalar<?, ?> ... sortedProps);

    <E> List<E> findByExample(Example<E> example, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    <E, V extends View<E>> List<V> findExample(Class<V> viewType, Example<E> example, TypedProp.Scalar<?, ?> ... sortedProps);

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
