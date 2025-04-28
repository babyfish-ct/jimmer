package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;

public interface DeprecatedSaveOperations extends SaveOperations {

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            E entity,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(input)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            SaveMode mode
    ) {
        return saveCommand(input)
                .setMode(mode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            E entity,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .setMode(mode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    @Deprecated
    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute(fetcher);
    }

    @Deprecated
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            Iterable<E> entities,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .setMode(mode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    @Deprecated
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute(viewType);
    }
    
    @Deprecated
    default <E> SimpleSaveResult<E> insert(@NotNull E entity) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> insert(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> insert(@NotNull Input<E> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> insert(@NotNull Input<E> input, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }
    
    @Deprecated
    default <E> SimpleSaveResult<E> insertIfAbsent(@NotNull E entity) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }
    
    @Deprecated
    default <E> SimpleSaveResult<E> insertIfAbsent(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }
    
    @Deprecated
    default <E> SimpleSaveResult<E> insertIfAbsent(@NotNull Input<E> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> insertIfAbsent(
            @NotNull Input<E> input,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> update(@NotNull E entity) {
        return saveCommand(entity)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }
    
    @Deprecated
    default <E> SimpleSaveResult<E> update(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> update(@NotNull Input<E> input) {
        return saveCommand(input)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> update(@NotNull Input<E> input, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(input)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> SimpleSaveResult<E> merge(@NotNull E entity) {
        return saveCommand(entity)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }


    @Deprecated
    default <E> SimpleSaveResult<E> merge(@NotNull Input<E> input) {
        return saveCommand(input)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> insertEntities(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> insertEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> insertInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> insertInputs(@NotNull Iterable<? extends Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> insertEntitiesIfAbsent(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> insertEntitiesIfAbsent(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> insertInputsIfAbsent(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> insertInputsIfAbsent(@NotNull Iterable<? extends Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> updateEntities(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> updateEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> updateInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> updateInputs(@NotNull Iterable<? extends Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }

    @Deprecated
    default <E> BatchSaveResult<E> mergeEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }
    
    @Deprecated
    default <E> BatchSaveResult<E> mergeInputs(@NotNull Iterable<? extends Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    @Deprecated
    default <E> SimpleSaveResult<E> save(
            E entity,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            Iterable<E> entities,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .execute(viewType);
    }

    /**
     * @deprecated `fetcher/viewType` is advanced feature,
     *              please call `saveCommand().execute(...fetcher/viewType...)`
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }
}
