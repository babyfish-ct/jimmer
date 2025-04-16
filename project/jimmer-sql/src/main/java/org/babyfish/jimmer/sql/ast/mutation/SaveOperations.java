package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;

public interface SaveOperations extends SaveCommandCreator {

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
}
