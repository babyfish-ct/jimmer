package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.*
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
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.runtime.EntityManager
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

    val loaders: KLoaders

    fun getAssociations(prop: KProperty1<*, *>): KAssociations

    @NewChain
    fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient

    @NewChain
    fun filters(block: KFilterDsl.() -> Unit): KSqlClient

    @NewChain
    fun disableSlaveConnectionManager(): KSqlClient

    val entityManager: EntityManager

    val binLog: BinLog

    val javaClient: JSqlClientImplementor

    fun <E: Any> findById(entityType: KClass<E>, id: Any): E? =
        entities.findById(entityType, id)

    fun <E: Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        entities.findById(fetcher, id)

    fun <K, V: Any> findByIds(entityType: KClass<V>, ids: Collection<K>): Map<K, V> =
        entities.findMapByIds(entityType, ids)

    fun <K, V: Any> findByIds(fetcher: Fetcher<V>, ids: Collection<K>): Map<K, V> =
        entities.findMapByIds(fetcher, ids)

    fun <E: Any> save(entity: E, mode: SaveMode = SaveMode.UPSERT): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(mode)
            setAutoAttachingAll()
        }

    fun <E: Any> save(entity: E, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<E> =
        entities.save(entity, block = block)

    fun <E: Any> insert(entity: E): KSimpleSaveResult<E> =
        save(entity, SaveMode.INSERT_ONLY)

    fun <E: Any> update(entity: E): KSimpleSaveResult<E> =
        save(entity, SaveMode.UPDATE_ONLY)

    fun <E: Any> save(input: Input<E>, mode: SaveMode = SaveMode.UPSERT): KSimpleSaveResult<E> =
        save(input.toEntity(), mode)

    fun <E: Any> save(input: Input<E>, block: KSaveCommandDsl.() -> Unit): KSimpleSaveResult<E> =
        entities.save(input.toEntity(), block = block)

    fun <E: Any> insert(input: Input<E>): KSimpleSaveResult<E> =
        save(input.toEntity(), SaveMode.INSERT_ONLY)

    fun <E: Any> update(input: Input<E>): KSimpleSaveResult<E> =
        save(input.toEntity(), SaveMode.UPDATE_ONLY)

    fun <E: Any> deleteById(entityType: KClass<E>, id: Any, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.delete(entityType, id) {
            setMode(mode)
        }

    fun <E: Any> deleteById(entityType: KClass<E>, id: Any, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.delete(entityType, id, block = block)

    fun <E: Any> deleteByIds(entityType: KClass<E>, ids: Collection<*>, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.batchDelete(entityType, ids) {
            setMode(mode)
        }

    fun <E: Any> deleteByIds(entityType: KClass<E>, ids: Collection<*>, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.batchDelete(entityType, ids, block = block)
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val javaBuilder = JSqlClient.newBuilder()
    val dsl = KSqlClientDsl(javaBuilder)
    dsl.block()
    return dsl.buildKSqlClient()
}

fun JSqlClient.toKSqlClient(): KSqlClient =
    KSqlClientImpl(this as JSqlClientImplementor)