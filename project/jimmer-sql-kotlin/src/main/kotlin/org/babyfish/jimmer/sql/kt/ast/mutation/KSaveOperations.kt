package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

interface KSaveOperations : KSaveCommandCreator {

    fun <E: Any> save(
        entity: E,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, block)
            .execute()

    fun <E: Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, mode, associatedMode, block)
            .execute()

    fun <E: Any> saveEntities(
        entities: Iterable<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, block)
            .execute()

    fun <E: Any> saveEntities(
        entities: Iterable<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute()

    fun <E: Any> save(
        input: Input<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, block)
            .execute()

    fun <E: Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, mode, associatedMode, block)
            .execute()

    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, block)
            .execute()

    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute()
}