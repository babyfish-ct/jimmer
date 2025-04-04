package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KSaveOptions : KSaveCommandCreator {

    fun <E: Any> save(
        entity: E ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, block)
            .execute(fetcher)

    fun <E: Any> save(
        entity: E ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, mode, associatedMode, block)
            .execute(fetcher)

    fun <E: Any> saveEntities(
        entities: Iterable<E> ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, block)
            .execute(fetcher)

    fun <E: Any> saveEntities(
        entities: Iterable<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute(fetcher)

    fun <E: Any> save(
        input: Input<E> ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, block)
            .execute(fetcher)

    fun <E: Any> save(
        input: Input<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, mode, associatedMode, block)
            .execute(fetcher)

    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>> ,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, block)
            .execute(fetcher)

    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute(fetcher)

    fun <E: Any, V: View<E>> save(
        entity: E ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> save(
        entity: E ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, mode, associatedMode, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> saveEntities(
        entities: Iterable<E> ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> saveEntities(
        entities: Iterable<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> save(
        input: Input<E> ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(input, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> save(
        input: Input<E> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(input, mode, associatedMode, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>> ,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveInputsCommand(inputs, block)
            .execute(viewType)

    fun <E: Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>> ,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute(viewType)
}