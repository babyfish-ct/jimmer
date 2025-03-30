package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import java.sql.Connection
import kotlin.reflect.KClass

interface KDeprecatedMoreSaveOptions : KDeprecatedSaveOptions {

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        entity: E,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, mode, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        input: Input<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, mode, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        entity: E,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, mode, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(entity, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        input: Input<E>,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, mode, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult<E> {
        return super.save(input, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any, V : View<E>> save(
        entity: E,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(entity, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any, V : View<E>> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(entity, mode, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any, V : View<E>> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(entity, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any, V : View<E>> save(
        input: Input<E>,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(input, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any, V : View<E>> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(input, mode, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.save(...)"))
    override fun <E : Any, V : View<E>> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KSimpleSaveResult.View<E, V> {
        return super.save(input, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, mode, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, mode, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, associatedMode, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, mode, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any> saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveEntities(entities, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, mode, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult<E> {
        return super.saveInputs(inputs, associatedMode, fetcher, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any, V : View<E>> saveEntities(
        entities: Iterable<E>,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveEntities(entities, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any, V : View<E>> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveEntities(entities, mode, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveEntities(...)"))
    override fun <E : Any, V : View<E>> saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveEntities(entities, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any, V : View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveInputs(inputs, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any, V : View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveInputs(inputs, mode, associatedMode, viewType, con, block)
    }

    @Deprecated("will be removed", ReplaceWith("sqlClient.entities.saveInputs(...)"))
    override fun <E : Any, V : View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection?,
        block: (KSaveCommandPartialDsl.() -> Unit)?
    ): KBatchSaveResult.View<E, V> {
        return super.saveInputs(inputs, associatedMode, viewType, con, block)
    }
}