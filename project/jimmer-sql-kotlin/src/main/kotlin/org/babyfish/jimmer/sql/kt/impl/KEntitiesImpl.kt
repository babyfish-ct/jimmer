package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.Entities
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.fetcher.ViewMetadata
import org.babyfish.jimmer.sql.kt.KEntities
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.*
import org.babyfish.jimmer.sql.kt.ast.query.KExample
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose
import java.sql.Connection
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal class KEntitiesImpl(
    private val javaEntities: Entities
): KEntities {

    override fun forUpdate(): KEntities =
        javaEntities.forUpdate().let {
            if (javaEntities === it) {
                this
            } else {
                KEntitiesImpl(it)
            }
        }

    override fun forConnection(con: Connection?): KEntities =
        javaEntities.forConnection(con).let {
            if (javaEntities === it) {
                this
            } else {
                KEntitiesImpl(it)
            }
        }

    override fun <E : Any> findById(type: KClass<E>, id: Any): E? =
        javaEntities.findById(type.java, id)

    override fun <E : Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        javaEntities.findById(fetcher, id)

    override fun <E : Any> findByIds(
        type: KClass<E>,
        ids: Collection<*>
    ): List<E> =
        javaEntities.findByIds(type.java, ids)

    override fun <E : Any> findByIds(
        fetcher: Fetcher<E>,
        ids: Collection<*>
    ): List<E> =
        javaEntities.findByIds(fetcher, ids)

    override fun <ID, E : Any> findMapByIds(
        type: KClass<E>,
        ids: Collection<ID>
    ): Map<ID, E> =
        javaEntities.findMapByIds(type.java, ids)

    override fun <ID, E : Any> findMapByIds(
        fetcher: Fetcher<E>,
        ids: Collection<ID>
    ): Map<ID, E> =
        javaEntities.findMapByIds(fetcher, ids)

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> findAll(type: KClass<E>): List<E> =
        if (type.isSubclassOf(View::class)) {
            find(ViewMetadata.of(type.java as Class<out View<Any>>), null, null) as List<E>
        } else {
            find(ImmutableType.get(type.java), null, null, null)
        }

    override fun <E : Any> findAll(entityType: KClass<E>, block: (SortDsl<E>.() -> Unit)): List<E> =
        if (entityType.isSubclassOf(View::class)) {
            throw IllegalArgumentException("The argument cannot be view type, please call `findAllViews`")
        } else {
            find(ImmutableType.get(entityType.java), null, null, block)
        }

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, V : View<E>> findAllViews(viewType: KClass<V>, block: SortDsl<E>.() -> Unit): List<V> =
        find(ViewMetadata.of(viewType.java), null, block as SortDsl<*>.() -> Unit)

    override fun <E : Any> findAll(fetcher: Fetcher<E>, block: (SortDsl<E>.() -> Unit)?): List<E> =
        find(fetcher.immutableType, fetcher, null, block)

    override fun <E : Any> findByExample(
        example: KExample<E>,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): List<E> =
        find(example.type, fetcher, example, block)

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, V : View<E>> findByExample(
        viewType: KClass<V>,
        example: KExample<E>,
        block: (SortDsl<E>.() -> Unit)?
    ): List<V> =
        find(ViewMetadata.of(viewType.java), example, block as SortDsl<*>.() -> Unit)

    private fun <E: Any> find(
        type: ImmutableType,
        fetcher: Fetcher<E>?,
        example: KExample<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): List<E> {
        if (fetcher !== null && fetcher.immutableType !== type) {
            throw IllegalArgumentException(
                "The type \"${fetcher.immutableType}\" of fetcher does not match the query type \"$type\""
            )
        }
        if (example !== null && example.type !== type) {
            throw IllegalArgumentException(
                "The type \"${example.type}\" of example does not match the query type \"$type\""
            )
        }
        val entities = javaEntities as EntitiesImpl
        val query = MutableRootQueryImpl<Table<E>>(entities.sqlClient, type, ExecutionPurpose.QUERY, false)
        val table = query.getTable<Table<E>>()
        query.where(example?.toPredicate(query.getTable()))
        if (block !== null) {
            val dsl = SortDsl<E>()
            dsl.block()
            dsl.applyTo(query)
        }
        return query.select(
            if (fetcher !== null) {
                table.fetch(fetcher)
            } else {
                table
            }
        ).execute(entities.con)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V: View<*>> find(
        metadata: ViewMetadata<*, V>,
        example: KExample<*>?,
        block: (SortDsl<*>.() -> Unit)?
    ): List<V> {
        val fetcher = metadata.getFetcher()
        val converter = metadata.getConverter() as Function<*, V>
        val type = fetcher.immutableType
        val entities = javaEntities as EntitiesImpl
        val query = MutableRootQueryImpl<Table<*>>(entities.sqlClient, type, ExecutionPurpose.QUERY, false)
        val table = query.getTable<Table<*>>()
        if (block !== null) {
            val dsl = SortDsl<Any>()
            dsl.block()
            dsl.applyTo(query)
        }
        return query.select(
            if (fetcher !== null) {
                FetcherSelectionImpl(table, fetcher, converter)
            } else {
                table as Selection<V>
            }
        ).execute(entities.con)
    }

    override fun <E : Any> save(
        entity: E,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> =
        javaEntities
            .saveCommand(entity)
            .let {
                if (block === null) {
                    it
                } else {
                    it.configure { cfg ->
                        KSaveCommandDslImpl(cfg).block()
                    }
                }
            }
            .execute(con)
            .let { KSimpleSaveResultImpl(it) }

    override fun <E : Any> save(
        input: Input<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleSaveResult<E> =
        save(input.toEntity(), con, block)

    override fun <E : Any> saveAll(
        entities: Collection<E>,
        con: Connection?,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchSaveResult<E> =
        javaEntities
            .batchSaveCommand(entities)
            .let {
                if (block === null) {
                    it
                } else {
                    it.configure { cfg ->
                        KSaveCommandDslImpl(cfg).block()
                    }
                }
            }
            .execute(con)
            .let { KBatchSaveResultImpl(it) }

    override fun delete(
        type: KClass<*>,
        id: Any,
        con: Connection?,
        block: (KDeleteCommandDsl.() -> Unit)?
    ): KDeleteResult =
        javaEntities
            .deleteCommand(type.java, id)
            .let {
                if (block === null) {
                    it
                } else {
                    it.configure { cfg ->
                        KDeleteCommandDslImpl(cfg).block()
                    }
                }
            }
            .execute(con)
            .let { KDeleteResultImpl(it) }

    override fun deleteAll(
        type: KClass<*>,
        ids: Collection<*>,
        con: Connection?,
        block: (KDeleteCommandDsl.() -> Unit)?
    ): KDeleteResult =
        javaEntities
            .batchDeleteCommand(type.java, ids)
            .let {
                if (block === null) {
                    it
                } else {
                    it.configure { cfg ->
                        KDeleteCommandDslImpl(cfg).block()
                    }
                }
            }
            .execute(con)
            .let { KDeleteResultImpl(it) }
}