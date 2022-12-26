package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.Entities
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KEntities
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KBatchSaveResultImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KDeleteCommandDslImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KDeleteResultImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KSaveCommandDslImpl
import org.babyfish.jimmer.sql.kt.ast.mutation.impl.KSimpleSaveResultImpl
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import org.babyfish.jimmer.sql.kt.ast.query.KExample
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose
import java.sql.Connection
import kotlin.reflect.KClass

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

    override fun <E : Any> findById(entityType: KClass<E>, id: Any): E? =
        javaEntities.findById(entityType.java, id)

    override fun <E : Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        javaEntities.findById(fetcher, id)

    override fun <E : Any> findByIds(
        entityType: KClass<E>,
        ids: Collection<*>
    ): List<E> =
        javaEntities.findByIds(entityType.java, ids)

    override fun <E : Any> findByIds(
        fetcher: Fetcher<E>,
        ids: Collection<*>
    ): List<E> =
        javaEntities.findByIds(fetcher, ids)

    override fun <ID, E : Any> findMapByIds(
        entityType: KClass<E>,
        ids: Collection<ID>
    ): Map<ID, E> =
        javaEntities.findMapByIds(entityType.java, ids)

    override fun <ID, E : Any> findMapByIds(
        fetcher: Fetcher<E>,
        ids: Collection<ID>
    ): Map<ID, E> =
        javaEntities.findMapByIds(fetcher, ids)

    override fun <E : Any> findAll(type: KClass<E>, block: (SortDsl<E>.() -> Unit)?): List<E> =
        find(ImmutableType.get(type.java), null, null, block)

    override fun <E : Any> findAll(fetcher: Fetcher<E>, block: (SortDsl<E>.() -> Unit)?): List<E> =
        find(fetcher.immutableType, fetcher, null, block)

    override fun <E : Any> findByExample(
        example: KExample<E>,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)?
    ): List<E> =
        find(example.type, fetcher, example, block)

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
        example?.applyTo(query)
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

    override fun <E : Any> batchSave(
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
        entityType: KClass<*>,
        id: Any,
        con: Connection?,
        block: (KDeleteCommandDsl.() -> Unit)?
    ): KDeleteResult =
        javaEntities
            .deleteCommand(entityType.java, id)
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

    override fun batchDelete(
        entityType: KClass<*>,
        ids: Collection<*>,
        con: Connection?,
        block: (KDeleteCommandDsl.() -> Unit)?
    ): KDeleteResult =
        javaEntities
            .batchDeleteCommand(entityType.java, ids)
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