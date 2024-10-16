package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.Executor
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSqlClient {

    fun <E: Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> =
        queries.forEntity(entityType, block)

    fun <E: Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): KExecutable<Int>

    fun <E: Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int>

    val queries: KQueries

    val entities: KEntities

    val caches: KCaches

    /**
     * This property is equivalent to `getTriggers(false)`
     */
    val triggers: KTriggers

    /**
     * <ul>
     *     <li>
     *         If trigger type is 'BINLOG_ONLY'
     *         <ul>
     *             <li>If `transaction` is true, throws exception</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *     </li>
     *     <li>
     *         If trigger type is 'TRANSACTION_ONLY', returns transaction trigger
     *         no matter what the `transaction` is
     *     </li>
     *     <li>
     *         If trigger type is 'BOTH'
     *         <ul>
     *             <li>If `transaction` is true, return transaction trigger</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *         Note that the objects returned by different parameters are independent of each other.
     *     </li>
     * </ul>
     * @param transaction
     * @return Trigger
     */
    fun getTriggers(transaction: Boolean): KTriggers

    val filters: KFilters

    fun getAssociations(prop: KProperty1<*, *>): KAssociations

    @NewChain
    fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient

    @NewChain
    fun filters(block: KFilterDsl.() -> Unit): KSqlClient

    @NewChain
    fun disableSlaveConnectionManager(): KSqlClient

    @NewChain
    fun executor(executor: Executor?): KSqlClient

    val entityManager: EntityManager

    val binLog: BinLog

    fun <E: Any> findById(type: KClass<E>, id: Any): E? =
        entities.findById(type, id)

    fun <E: Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        entities.findById(fetcher, id)

    fun <E: Any> findByIds(type: KClass<E>, ids: Iterable<*>): List<E> =
        entities.findByIds(type, ids)

    fun <E: Any> findByIds(fetcher: Fetcher<E>, ids: Iterable<*>): List<E> =
        entities.findByIds(fetcher, ids)

    fun <K, V: Any> findMapByIds(type: KClass<V>, ids: Iterable<K>): Map<K, V> =
        entities.findMapByIds(type, ids)

    fun <K, V: Any> findMapByIds(fetcher: Fetcher<V>, ids: Iterable<K>): Map<K, V> =
        entities.findMapByIds(fetcher, ids)

    fun <E: Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> save(
        entity: E,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input) {
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> save(
        input: Input<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input) {
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> insert(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> insertIfAbsent(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> update(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> merge(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> insert(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> insertIfAbsent(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> update(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> merge(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(associatedMode)
            if (block != null) {
                block(this)
            }
        }

    fun <E: Any> saveEntities(
        entities: Iterable<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveEntities(entities, null, block)

    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveInputs(inputs, null, block)

    @Deprecated("will be deleted in 0.9")
    fun <E: Any> merge(entity: E, mode: SaveMode): KSimpleSaveResult<E> =
        save(entity) {
            setAssociatedModeAll(AssociatedSaveMode.MERGE)
            setMode(mode)
        }

    /**
     * For associated objects, only insert or update operations are executed.
     * The parent object never dissociates the child objects.
     */
    @Deprecated("will be deleted in 0.9")
    fun <E: Any> merge(input: Input<E>, mode: SaveMode): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            setAssociatedModeAll(AssociatedSaveMode.MERGE)
            setMode(mode)
        }

    /**
     * For associated objects, only insert operations are executed.
     */
    @Deprecated("will be deleted in 0.9")
    fun <E: Any> append(entity: E, mode: SaveMode = SaveMode.INSERT_ONLY): KSimpleSaveResult<E> =
        save(entity) {
            setAssociatedModeAll(AssociatedSaveMode.APPEND)
            setMode(mode)
        }

    /**
     * For associated objects, only insert operations are executed.
     */
    @Deprecated("will be deleted in 0.9")
    fun <E: Any> append(input: Input<E>, mode: SaveMode = SaveMode.INSERT_ONLY): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            setAssociatedModeAll(AssociatedSaveMode.APPEND)
            setMode(mode)
        }

    /**
     * For associated objects, only insert operations are executed.
     */
    @Deprecated("will be deleted in 0.9")
    fun <E: Any> append(entity: E, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<E> =
        save(entity) {
            block()
            setAssociatedModeAll(AssociatedSaveMode.APPEND)
        }

    /**
     * For associated objects, only insert operations are executed.
     */
    @Deprecated("will be deleted in 0.9")
    fun <E: Any> append(input: Input<E>, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            block()
            setAssociatedModeAll(AssociatedSaveMode.APPEND)
        }

    fun <E: Any> deleteById(type: KClass<E>, id: Any, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.delete(type, id) {
            setMode(mode)
        }

    fun <E: Any> deleteById(type: KClass<E>, id: Any, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.delete(type, id, block = block)

    fun <E: Any> deleteByIds(type: KClass<E>, ids: Iterable<*>, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.deleteAll(type, ids) {
            setMode(mode)
        }

    fun <E: Any> deleteByIds(type: KClass<E>, ids: Iterable<*>, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.deleteAll(type, ids, block = block)

    val javaClient: JSqlClientImplementor
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val javaBuilder = JSqlClient.newBuilder()
    val dsl = KSqlClientDsl(javaBuilder)
    dsl.block()
    return dsl.buildKSqlClient()
}

fun JSqlClient.toKSqlClient(): KSqlClient =
    KSqlClientImpl(this as JSqlClientImplementor)