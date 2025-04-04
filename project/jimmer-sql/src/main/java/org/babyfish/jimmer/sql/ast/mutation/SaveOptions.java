package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface SaveOptions extends SaveCommandCreator {

    default <E> SimpleSaveResult<E> save(
            E entity
    ) {
        return saveCommand(entity)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities
    ) {
        return saveEntitiesCommand(entities)
                .execute();
    }

    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(
            Input<E> input
    ) {
        return saveCommand(input)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(input)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs
    ) {
        return saveInputsCommand(inputs)
                .execute();
    }

    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(
            E entity,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .execute(fetcher);
    }

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

    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .execute(fetcher);
    }

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

    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .execute(fetcher);
    }

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

    default <E> BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .execute(fetcher);
    }

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

    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .execute(viewType);
    }

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

    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            Iterable<E> entities,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .execute(viewType);
    }

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

    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .execute(viewType);
    }

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

    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .execute(viewType);
    }

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
