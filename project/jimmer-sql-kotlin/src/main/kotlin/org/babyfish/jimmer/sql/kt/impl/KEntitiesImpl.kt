package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.Entities
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.fetcher.DtoMetadata
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

    override fun <E : Any> findOneById(type: KClass<E>, id: Any): E =
        javaEntities.findOneById(type.java, id)

    override fun <E : Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        javaEntities.findById(fetcher, id)

    override fun <E : Any> findOneById(fetcher: Fetcher<E>, id: Any): E =
        javaEntities.findOneById(fetcher, id)

    override fun <E : Any> findByIds(
        type: KClass<E>,
        ids: Iterable<*>
    ): List<E> =
        javaEntities.findByIds(type.java, ids)

    override fun <E : Any> findByIds(
        fetcher: Fetcher<E>,
        ids: Iterable<*>
    ): List<E> =
        javaEntities.findByIds(fetcher, ids)

    override fun <ID, E : Any> findMapByIds(
        type: KClass<E>,
        ids: Iterable<ID>
    ): Map<ID, E> =
        javaEntities.findMapByIds(type.java, ids)

    override fun <ID, E : Any> findMapByIds(
        fetcher: Fetcher<E>,
        ids: Iterable<ID>
    ): Map<ID, E> =
        javaEntities.findMapByIds(fetcher, ids)

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> findAll(type: KClass<E>): List<E> =
        if (type.isSubclassOf(View::class)) {
            find(DtoMetadata.of(type.java as Class<out View<Any>>), null, null) as List<E>
        } else {
            find(ImmutableType.get(type.java), null, null, null)
        }

    override fun <E : Any> findAll(type: KClass<E>, block: (SortDsl<E>.() -> Unit)?): List<E> =
        if (type.isSubclassOf(View::class)) {
            throw IllegalArgumentException("The argument cannot be view type, please call `findAllViews`")
        } else {
            find(ImmutableType.get(type.java), null, null, block)
        }

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, V : View<E>> findAllViews(viewType: KClass<V>, block: (SortDsl<E>.() -> Unit)?): List<V> =
        find(DtoMetadata.of(viewType.java), null, block as (SortDsl<*>.() -> Unit)?)

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
        find(DtoMetadata.of(viewType.java), example, block as (SortDsl<*>.() -> Unit)?)

    @Suppress("UNCHECKED_CAST")
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
        val query = MutableRootQueryImpl<Table<E>>(entities.sqlClient, type, ExecutionPurpose.QUERY, FilterLevel.DEFAULT)
        val table = query.tableLikeImplementor as TableImplementor<E>
        query.where(example?.toPredicate(query.tableLikeImplementor as Table<*>))
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
        metadata: DtoMetadata<*, V>,
        example: KExample<*>?,
        block: (SortDsl<*>.() -> Unit)?
    ): List<V> {
        val fetcher = metadata.getFetcher()
        val converter = metadata.getConverter() as Function<*, V>
        val type = fetcher.immutableType
        val entities = javaEntities as EntitiesImpl
        val query = MutableRootQueryImpl<Table<*>>(entities.sqlClient, type, ExecutionPurpose.QUERY, FilterLevel.DEFAULT)
        val table = query.tableLikeImplementor as Table<*>
        if (example !== null) {
            query.where(example.toPredicate(table))
        }
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

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> saveCommand(entity: E, block: (KSaveCommandDsl.() -> Unit)?): KSimpleEntitySaveCommand<E> =
        KSimpleEntitySaveCommandImpl(
            javaEntities
                .saveCommand(entity)
                .let {
                    if (block === null) {
                        it
                    } else {
                        val dsl = KSaveCommandDslImpl(it)
                        dsl.block()
                        dsl.javaCommand as SimpleEntitySaveCommand<E>
                    }
                }
        )

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> saveEntitiesCommand(
        entities: Iterable<E>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchEntitySaveCommand<E> =
        KBatchEntitySaveCommandImpl(
            javaEntities
                .saveEntitiesCommand(entities)
                .let {
                    if (block === null) {
                        it
                    } else {
                        val dsl = KSaveCommandDslImpl(it)
                        dsl.block()
                        dsl.javaCommand as BatchEntitySaveCommand<E>
                    }
                }
        )

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
                    val dsl = KDeleteCommandDslImpl(it)
                    dsl.block()
                    dsl.javaCommand
                }
            }
            .execute(con)
            .let { KDeleteResultImpl(it) }

    override fun deleteAll(
        type: KClass<*>,
        ids: Iterable<*>,
        con: Connection?,
        block: (KDeleteCommandDsl.() -> Unit)?
    ): KDeleteResult =
        javaEntities
            .deleteAllCommand(type.java, ids)
            .let {
                if (block === null) {
                    it
                } else {
                    val dsl = KDeleteCommandDslImpl(it)
                    dsl.block()
                    dsl.javaCommand
                }
            }
            .execute(con)
            .let { KDeleteResultImpl(it) }
}