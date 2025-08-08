package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KDeprecatedMoreSaveOperations : KDeprecatedSaveOperations {

    @Deprecated(
        "Please use the function of `entities` with same features", 
        replaceWith = ReplaceWith("entities.saveInputs(inputs, mode, associatedMode, viewType, block)")
    )
    override fun <E : Any, V : View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveInputs(inputs, mode, associatedMode, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveInputs(inputs, viewType, block)")
    )
    override fun <E : Any, V : View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveInputs(inputs, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(input, mode, associatedMode, viewType, block)")
    )
    override fun <E : Any, V : View<E>> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(input, mode, associatedMode, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(input, viewType, block)")
    )
    override fun <E : Any, V : View<E>> save(
        input: Input<E>,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(input, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveEntities(entities, mode, associatedMode, viewType, block)")
    )
    override fun <E : Any, V : View<E>> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveEntities(entities, mode, associatedMode, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveEntities(entities, viewType, block)")
    )
    override fun <E : Any, V : View<E>> saveEntities(
        entities: Iterable<E>,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveEntities(entities, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(entity, mode, associatedMode, viewType, block)")
    )
    override fun <E : Any, V : View<E>> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(entity, mode, associatedMode, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(entity, viewType, block)")
    )
    override fun <E : Any, V : View<E>> save(
        entity: E,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(entity, viewType, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveInputs(inputs, mode, associatedMode, fetcher, block)")
    )
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, mode, associatedMode, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveInputs(inputs, fetcher, block)")
    )
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(input, mode, associatedMode, fetcher, block)")
    )
    override fun <E : Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, mode, associatedMode, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(input, fetcher, block)")
    )
    override fun <E : Any> save(
        input: Input<E>,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveEntities(entities, mode, associatedMode, fetcher, block)")
    )
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, mode, associatedMode, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.saveEntities(entities, mode, associatedMode, fetcher, block)")
    )
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(entity, mode, associatedMode, fetcher, block)")
    )
    override fun <E : Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, mode, associatedMode, fetcher, block)
    }

    @Deprecated(
        "Please use the function of `entities` with same features",
        replaceWith = ReplaceWith("entities.save(entity, fetcher, block)")
    )
    override fun <E : Any> save(
        entity: E,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, fetcher, block)
    }
}
