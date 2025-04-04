package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface DeprecatedMoreSaveOptions extends DeprecatedSaveOptions {

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity) {
        return DeprecatedSaveOptions.super.save(entity);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.save(entity, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities) {
        return DeprecatedSaveOptions.super.saveEntities(entities);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return DeprecatedSaveOptions.super.save(input);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.save(input, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs) {
        return DeprecatedSaveOptions.super.saveInputs(inputs);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(E entity, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(entity, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(entity, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveEntities(entities, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(input, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.save(input, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(entity, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(entity, mode, associatedMode, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(Iterable<E> entities, Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveEntities(entities, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(Iterable<E> entities, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveEntities(entities, mode, associatedMode, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(input, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.save(input, mode, associatedMode, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(Iterable<? extends Input<E>> inputs, Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, viewType);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOptions.super.saveInputs(inputs, mode, associatedMode, viewType);
    }
}
