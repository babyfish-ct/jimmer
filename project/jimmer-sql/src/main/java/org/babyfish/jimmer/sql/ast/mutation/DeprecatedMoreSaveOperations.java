package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface DeprecatedMoreSaveOperations extends DeprecatedSaveOperations {

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Deprecated
    @Override
    default <E> SimpleSaveResult<E> save(E entity) {
        return DeprecatedSaveOperations.super.save(entity);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOperations.super.save(entity, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities) {
        return DeprecatedSaveOperations.super.saveEntities(entities);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOperations.super.saveEntities(entities, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return DeprecatedSaveOperations.super.save(input);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOperations.super.save(input, mode, associatedMode);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs) {
        return DeprecatedSaveOperations.super.saveInputs(inputs);
    }

    /**
     * @deprecated Please call the method with same features under {@code .getEntities()}
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode, AssociatedSaveMode associatedMode) {
        return DeprecatedSaveOperations.super.saveInputs(inputs, mode, associatedMode);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> SimpleSaveResult<E> save(E entity, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.save(entity, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.save(entity, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.saveEntities(entities, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.saveEntities(entities, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.save(input, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.save(input, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.saveInputs(inputs, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E> BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode, AssociatedSaveMode associatedMode, Fetcher<E> fetcher) {
        return DeprecatedSaveOperations.super.saveInputs(inputs, mode, associatedMode, fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, Class<V> viewType) {
        return DeprecatedSaveOperations.super.save(entity, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOperations.super.save(entity, mode, associatedMode, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(Iterable<E> entities, Class<V> viewType) {
        return DeprecatedSaveOperations.super.saveEntities(entities, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(Iterable<E> entities, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOperations.super.saveEntities(entities, mode, associatedMode, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, Class<V> viewType) {
        return DeprecatedSaveOperations.super.save(input, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOperations.super.save(input, mode, associatedMode, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(Iterable<? extends Input<E>> inputs, Class<V> viewType) {
        return DeprecatedSaveOperations.super.saveInputs(inputs, viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Override
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode, AssociatedSaveMode associatedMode, Class<V> viewType) {
        return DeprecatedSaveOperations.super.saveInputs(inputs, mode, associatedMode, viewType);
    }
}
