package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;

public interface DeprecatedMoreSaveOptions extends DeprecatedSaveOptions {

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, @NotNull AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.save(entity, mode, associatedMode);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return DeprecatedSaveOptions.super.save(entity, mode);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.save(entity, associatedMode);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity) {
        return DeprecatedSaveOptions.super.save(entity);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.save(input, mode, associatedMode);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode) {
        return DeprecatedSaveOptions.super.save(input, mode);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.save(input, associatedMode);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return DeprecatedSaveOptions.super.save(input);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, @NotNull AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(entity, mode, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(entity, mode, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(entity, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(entity, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(input, mode, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(input, mode, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(input, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(input, fetcher);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(entity, mode, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, SaveMode mode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(entity, mode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(entity, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(entity, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(input, mode, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, SaveMode mode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(input, mode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(input, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(input, viewType);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode, @NotNull AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, associatedMode);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.saveEntities(entities, associatedMode);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities) {
        return DeprecatedSaveOptions.super.saveEntities(entities);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode, @NotNull AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, associatedMode);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, associatedMode);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs) {
        return DeprecatedSaveOptions.super.saveInputs(inputs);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode, @NotNull AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveEntities(entities, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveEntities(entities, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode, @NotNull AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, associatedMode, fetcher);
    }

    @Deprecated
    @Override
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, fetcher);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode, @NotNull AssociatedSaveMode associatedMode, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(@NotNull Iterable<E> entities, @NotNull AssociatedSaveMode associatedMode, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveEntities(entities, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(@NotNull Iterable<E> entities, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveEntities(entities, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode, @NotNull AssociatedSaveMode associatedMode, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull AssociatedSaveMode associatedMode, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, associatedMode, viewType);
    }

    @Deprecated
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, viewType);
    }
}
