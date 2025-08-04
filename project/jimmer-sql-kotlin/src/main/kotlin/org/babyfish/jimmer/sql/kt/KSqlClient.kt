package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.View
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.exception.DatabaseValidationException
import org.babyfish.jimmer.sql.exception.EmptyResultException
import org.babyfish.jimmer.sql.exception.TooManyResultsException
import org.babyfish.jimmer.sql.fetcher.DtoMetadata
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.query.*
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl
import org.babyfish.jimmer.sql.runtime.ConnectionManager
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.Executor
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import org.babyfish.jimmer.sql.transaction.AbstractTxConnectionManager
import org.babyfish.jimmer.sql.transaction.Propagation
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSqlClient : KDeprecatedMoreSaveOperations {

    fun <E : Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery.ForEntity<E>.() -> KConfigurableRootQuery<KNonNullTable<E>, R>
    ): KConfigurableRootQuery<KNonNullTable<E>, R> =
        queries.forEntity(entityType, block)

    fun <B: KNonNullBaseTable<*>, R> createQuery(
        symbol: KBaseTableSymbol<B>,
        block: KMutableRootQuery<B>.() -> KConfigurableRootQuery<B, R>
    ): KConfigurableRootQuery<B, R>

    fun <E: Any, B: KNonNullBaseTable<*>> createBaseQuery(
        entityType: KClass<E>,
        block: KMutableBaseQuery<E>.() -> KConfigurableBaseQuery<B>
    ): KConfigurableBaseQuery<B>

    fun <E: Any, B: KNonNullBaseTable<*>> createBaseQuery(
        entityType: KClass<E>,
        recursiveRef: KRecursiveRef<B>,
        joinBlock: KPropsWeakJoinFun<KNonNullTable<E>, B>,
        block: KMutableRecursiveBaseQuery<E, B>.() -> KConfigurableBaseQuery<B>
    ): KConfigurableBaseQuery<B>

    fun <E: Any, B: KNonNullBaseTable<*>> createBaseQuery(
        entityType: KClass<E>,
        recursiveRef: KRecursiveRef<B>,
        weakJoinType: KClass<out KPropsWeakJoin<KNonNullTable<E>, B>>,
        block: KMutableRecursiveBaseQuery<E, B>.() -> KConfigurableBaseQuery<B>
    ): KConfigurableBaseQuery<B>

    fun <E : Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): KExecutable<Int>

    fun <E : Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int>

    fun <E : Any, R> executeQuery(
        entityType: KClass<E>,
        limit: Int? = null,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> KConfigurableRootQuery<KNonNullTable<E>, R>
    ): List<R> = queries
        .forEntity(entityType, block)
        .let { q ->
            limit?.let { q.limit(it) } ?: q
        }
        .execute(con)

    fun <E : Any> executeUpdate(
        entityType: KClass<E>,
        con: Connection? = null,
        block: KMutableUpdate<E>.() -> Unit
    ): Int = createUpdate(entityType, block).execute(con)

    fun <E : Any> executeDelete(
        entityType: KClass<E>,
        con: Connection? = null,
        block: KMutableDelete<E>.() -> Unit
    ): Int = createDelete(entityType, block).execute(con)

    val queries: KQueries

    val entities: KEntities

    val caches: KCaches

    /**
     * This property is equivalent to `getTriggers(false)`
     */
    val triggers: KTriggers

    /**
     * -   If trigger type is 'BINLOG_ONLY'
     *     -   If `transaction` is true, throws exception
     *     -   If `transaction` is false, return binlog trigger
     *
     * -   If trigger type is 'TRANSACTION_ONLY',
     * returns transaction trigger no matter what the `transaction` is
     *
     * -   If trigger type is 'BOTH'
     *     -   If `transaction` is true, return transaction trigger
     *     -   If `transaction` is false, return binlog trigger
     *     >   Note that the objects returned by different parameters are independent of each other.
     *
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

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <T : Any> findById(type: KClass<T>, id: Any): T? =
        entities.findById(type, id)

    fun <E : Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        entities.findById(fetcher, id)

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <T : Any> findByIds(type: KClass<T>, ids: Iterable<*>): List<T> =
        entities.findByIds(type, ids)

    fun <E : Any> findByIds(fetcher: Fetcher<E>, ids: Iterable<*>): List<E> =
        entities.findByIds(fetcher, ids)

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <K, T : Any> findMapByIds(type: KClass<T>, ids: Iterable<K>): Map<K, T> =
        entities.findMapByIds(type, ids)

    fun <K, V : Any> findMapByIds(fetcher: Fetcher<V>, ids: Iterable<K>): Map<K, V> =
        entities.findMapByIds(fetcher, ids)

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <T : Any> findOneById(type: KClass<T>, id: Any): T =
        entities.findOneById(type, id)

    fun <E : Any> findOneById(fetcher: Fetcher<E>, id: Any): E =
        entities.findOneById(fetcher, id)

    fun <E : Any> findAll(
        fetcher: Fetcher<E>,
        limit: Int? = null,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> Unit = {}
    ): List<E> = executeQuery(fetcher.javaClass.kotlin, limit, con) {
        block()
        select(table.fetch(fetcher))
    }

    fun <E : Any> findOne(
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> Unit
    ): E = findAll(fetcher, 2, null, block).let {
        when (it.size) {
            0 -> throw EmptyResultException()
            1 -> it[0]
            else -> throw TooManyResultsException()
        }
    }

    fun <E : Any> findOneOrNull(
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> Unit
    ): E? = findAll(fetcher, 2, con, block).let {
        when (it.size) {
            0 -> null
            1 -> return it[0]
            else -> throw TooManyResultsException()
        }
    }

    fun <E : Any, V : View<E>> findAll(
        viewType: KClass<V>,
        limit: Int? = null,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> Unit = {}
    ): List<V> {
        val metadata = DtoMetadata.of(viewType.java)
        return findAll(metadata.fetcher, limit, con, block).map(metadata.converter::apply)
    }

    fun <E : Any, V : View<E>> findOne(
        viewType: KClass<V>,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> Unit
    ): V {
        val metadata = DtoMetadata.of(viewType.java)
        return findOne(metadata.fetcher, con, block).let(metadata.converter::apply)
    }

    fun <E : Any, V : View<E>> findOneOrNull(
        viewType: KClass<V>,
        con: Connection? = null,
        block: KMutableRootQuery.ForEntity<E>.() -> Unit
    ): V? {
        val metadata = DtoMetadata.of(viewType.java)
        return findOneOrNull(metadata.fetcher, con, block)?.let(metadata.converter::apply)
    }

    override fun <E : Any> saveCommand(
        entity: E,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KSimpleEntitySaveCommand<E> =
        entities.saveCommand(entity, block)

    override fun <E : Any> saveEntitiesCommand(
        entities: Iterable<E>,
        block: (KSaveCommandDsl.() -> Unit)?
    ): KBatchEntitySaveCommand<E> =
        this.entities.saveEntitiesCommand(entities, block)

    fun <E : Any> deleteById(type: KClass<E>, id: Any, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.delete(type, id) {
            setMode(mode)
        }

    fun <E : Any> deleteById(type: KClass<E>, id: Any, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.delete(type, id, block = block)

    fun <E : Any> deleteByIds(type: KClass<E>, ids: Iterable<*>, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.deleteAll(type, ids) {
            setMode(mode)
        }

    fun <E : Any> deleteByIds(type: KClass<E>, ids: Iterable<*>, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.deleteAll(type, ids, block = block)

    /**
     * Execute a transaction by the specified [Propagation] behavior
     *
     * -   If an IOC framework is used, its implementation
     * should be an encapsulation of the transaction management
     * within the IOC framework. Taking `jimmer-spring-starter`
     * as an example, it is the `SpringConnectionManager`
     * which will be created and enabled automatically.
     *
     * -   If no IOC framework is used, the class
     * [AbstractTxConnectionManager] is the
     * lightweight implementation provided by jimmer,
     * please specify the connection manager of sqlClient
     * by [ConnectionManager.simpleConnectionManager]
     *
     * @param propagation The propagation behavior
     * @param block The action to be executed in transaction
     * @return The result of transaction
     */
    fun <R> transaction(propagation: Propagation = Propagation.REQUIRED, block: () -> R): R

    /**
     * Validate the database manually.
     *
     * User can either automatically validate the database or manually validate it.
     *
     * - Automatically:
     *   Specify the `databaseValidationMode` as
     *   [DatabaseValidationMode.ERROR] or [DatabaseValidationMode.WARNING]
     *   when building the `KSqlClient` object, so there is **NO** need
     *   to call this function
     *
     * - Manually: Specify the `databaseValidationMode` as
     *   [DatabaseValidationMode.NONE] and call this method after obtaining
     *   the `KSqlClient` object
     *
     * > Note: If there are any database validation errors, the relevant
     * exceptions will be returned as the result of this method
     * instead of being thrown directly.
     *
     * @return The validation error or null
     */
    fun validateDatabase(): DatabaseValidationException?

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
