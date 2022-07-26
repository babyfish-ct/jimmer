package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Entities {

    @NewChain
    @NotNull
    Entities forUpdate();

    @NewChain
    @NotNull
    Entities forConnection(@Nullable Connection con);

    @Nullable
    <E> E findById(@NotNull Class<E> entityType, @NotNull Object id);

    @NotNull
    <E> List<E> findByIds(@NotNull Class<E> entityType, @NotNull Collection<?> ids);

    @NotNull
    <ID, E> Map<ID, E> findMapByIds(@NotNull Class<E> entityType, @NotNull Collection<ID> ids);

    @Nullable
    <E> E findById(@NotNull Fetcher<E> fetcher, @NotNull Object id);

    @NotNull
    <E> List<E> findByIds(@NotNull Fetcher<E> fetcher, @NotNull Collection<?> ids);

    @NotNull
    <ID, E> Map<ID, E> findMapByIds(@NotNull Fetcher<E> fetcher, @NotNull Collection<ID> ids);

    @NotNull
    <E> SimpleSaveResult<E> save(@NotNull E entity);

    @NotNull
    <E> SimpleEntitySaveCommand<E> saveCommand(@NotNull E entity);

    @NotNull
    <E> BatchSaveResult<E> batchSave(@NotNull Collection<E> entities);

    @NotNull
    <E> BatchEntitySaveCommand<E> batchSaveCommand(@NotNull Collection<E> entities);

    @NotNull
    DeleteResult delete(@NotNull Class<?> entityType, @NotNull Object id);

    @NotNull
    DeleteCommand deleteCommand(@NotNull Class<?> entityType, @NotNull Object id);

    @NotNull
    DeleteResult batchDelete(@NotNull Class<?> entityType, @NotNull Collection<?> ids);

    @NotNull
    DeleteCommand batchDeleteCommand(@NotNull Class<?> entityType, @NotNull Collection<?> ids);
}
