package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

interface KSaveCommandCreator {

    fun <E: Any> saveCommand(
        entity: E,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E>

    fun <E: Any> saveCommand(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E> = saveCommand(entity) {
        setMode(mode)
        setAssociatedModeAll(associatedMode)
        block?.invoke(this)
    }

    fun <E: Any> saveCommand(
        input: Input<E>,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E> =
        saveCommand(input.toEntity(), block)

    fun <E: Any> saveCommand(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KSimpleEntitySaveCommand<E> =
        saveCommand(input.toEntity()) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> saveEntitiesCommand(
        entities: Iterable<E>,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E>

    fun <E: Any> saveEntitiesCommand(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E> = saveEntitiesCommand(entities) {
        setMode(mode)
        setAssociatedModeAll(associatedMode)
        block?.invoke(this)
    }

    fun <E: Any> saveInputsCommand(
        input: Iterable<Input<E>>,
        block: (KSaveCommandDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E> =
        saveEntitiesCommand(input.map { it.toEntity() }, block)

    fun <E: Any> saveInputsCommand(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit) ?= null
    ): KBatchEntitySaveCommand<E> =
        saveEntitiesCommand(inputs.map { it.toEntity() }) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }
}