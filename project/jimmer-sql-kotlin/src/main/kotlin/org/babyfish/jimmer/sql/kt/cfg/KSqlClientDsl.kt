package org.babyfish.jimmer.sql.kt.cfg

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.EnumType
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback
import org.babyfish.jimmer.sql.cache.CacheConfig
import org.babyfish.jimmer.sql.cache.CacheFactory
import org.babyfish.jimmer.sql.dialect.Dialect
import org.babyfish.jimmer.sql.event.TriggerType
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.impl.JavaCustomizer
import org.babyfish.jimmer.sql.kt.cfg.impl.JavaInitializer
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.impl.toJavaFilter
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl
import org.babyfish.jimmer.sql.meta.DatabaseNamingStrategy
import org.babyfish.jimmer.sql.meta.IdGenerator
import org.babyfish.jimmer.sql.runtime.*
import java.sql.Connection
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@DslScope
class KSqlClientDsl internal constructor(
    val javaBuilder: JSqlClient.Builder
) {
    fun setDialect(dialect: Dialect) {
        javaBuilder.setDialect(dialect)
    }

    fun setDefaultEnumStrategy(strategy: EnumType.Strategy) {
        javaBuilder.setDefaultEnumStrategy(strategy)
    }

    fun setDatabaseNamingStrategy(strategy: DatabaseNamingStrategy) {
        javaBuilder.setDatabaseNamingStrategy(strategy)
    }

    fun setDefaultBatchSize(size: Int) {
        javaBuilder.setDefaultBatchSize(size)
    }

    fun setDefaultListBatchSize(size: Int) {
        javaBuilder.setDefaultListBatchSize(size)
    }

    fun setConnectionManager(block: ConnectionManagerDsl.() -> Unit) {
        javaBuilder.setConnectionManager(ConnectionManagerImpl(block))
    }

    fun setConnectionManager(connectionManager: ConnectionManager) {
        javaBuilder.setConnectionManager(connectionManager)
    }

    fun setSlaveConnectionManager(block: ConnectionManagerDsl.() -> Unit) {
        javaBuilder.setSlaveConnectionManager(ConnectionManagerImpl(block))
    }

    fun setSlaveConnectionManager(connectionManager: ConnectionManager) {
        javaBuilder.setSlaveConnectionManager(connectionManager)
    }

    fun setExecutor(executor: Executor?) {
        javaBuilder.setExecutor(executor)
    }

    /**
     * If this option is configured, when jimmer calls back
     * `org.babyfish.jimmer.sql.runtime.Executor.execute` before executing SQL,
     * it will check the stack trace information of the current thread.
     *
     * However, these stack traces have too much information, including
     * infrastructure call frames represented by jdk, jdbc driver, jimmer, and spring,
     * and the business-related information you care about will be submerged in the ocean of information.
     *
     * Through this configuration, you can specify multiple package or class prefixes, and jimmer will
     * judge whether there are some call frames in the stack trace whose class names start with some
     * of these prefixes. If the judgment is true, jimmer believes that the current callback is related
     * to your business, and the `ctx` parameter of `org.babyfish.jimmer.sql.runtime.Executor.execute`
     * will be passed as non-null.
     *
     * If the SQL logging configuration is enabled at the same time, when a SQL statement is caused by
     * the business you care about, the business call frame will be printed together with the SQL log.
     */
    fun setExecutorContextPrefixes(prefixes: Collection<String>) {
        javaBuilder.setExecutorContextPrefixes(prefixes)
    }

    fun setTransientResolverProvider(provider: TransientResolverProvider) {
        javaBuilder.setTransientResolverProvider(provider)
    }

    fun setIdGenerator(idGenerator: IdGenerator) {
        javaBuilder.setIdGenerator(idGenerator)
    }

    fun setIdGenerator(entityType: KClass<*>, idGenerator: IdGenerator) {
        javaBuilder.setIdGenerator(entityType.java, idGenerator)
    }

    fun addScalarProvider(scalarProvider: ScalarProvider<*, *>) {
        javaBuilder.addScalarProvider(scalarProvider)
    }

    fun addScalarProvider(prop: KProperty1<*, *>, scalarProvider: ScalarProvider<*, *>) {
        javaBuilder.addScalarProvider(prop.toImmutableProp(), scalarProvider)
    }

    fun addScalarProvider(prop: ImmutableProp, scalarProvider: ScalarProvider<*, *>) {
        javaBuilder.addScalarProvider(prop, scalarProvider)
    }

    /**
     * Under normal circumstances, users do not need to set the entity manager.
     *
     * This configuration is for compatibility with version 0.7.47 and earlier.
     */
    fun setEntityManager(entityManager: EntityManager) {
        javaBuilder.setEntityManager(entityManager)
    }

    fun setCaches(block: CacheDsl.() -> Unit) {
        javaBuilder.setCaches {
            CacheDsl(it).block()
        }
    }

    fun setTriggerType(triggerType: TriggerType) {
        javaBuilder.setTriggerType(triggerType)
    }

    fun addFilters(vararg filters: KFilter<*>) {
        javaBuilder.addFilters(filters.map { it.toJavaFilter() })
    }

    fun addFilters(filters: Collection<KFilter<*>>) {
        javaBuilder.addFilters(filters.map { it.toJavaFilter() })
    }

    fun addDisabledFilters(vararg filters: KFilter<*>) {
        javaBuilder.addDisabledFilters(filters.map { it.toJavaFilter() })
    }

    fun addDisabledFilters(filters: Collection<KFilter<*>>) {
        javaBuilder.addDisabledFilters(filters.map { it.toJavaFilter() })
    }

    fun ignoreBuiltInFilters() {
        javaBuilder.ignoreBuiltInFilters()
    }

    fun addDraftInterceptor(interceptor: DraftInterceptor<*>) {
        javaBuilder.addDraftInterceptor(interceptor)
    }

    fun addDraftInterceptors(vararg interceptors: DraftInterceptor<*>) {
        javaBuilder.addDraftInterceptors(*interceptors)
    }

    fun addDraftInterceptors(interceptor: List<DraftInterceptor<*>>) {
        javaBuilder.addDraftInterceptors(interceptor)
    }

    fun setBinLogObjectMapper(mapper: ObjectMapper) {
        javaBuilder.setBinLogObjectMapper(mapper)
    }

    /**
     * This configuration is only useful for {@link org.babyfish.jimmer.sql.JoinColumn}
     * of local associations (not remote associations across microservice boundaries)
     * whose `foreignKeyType` is specified as `AUTO`.Its value indicates whether the
     * foreign key is real, that is, whether there is a foreign key constraint in the database.
     *
     * <p>In general, you should ignore this configuration (defaults to true) or set it to true.</p>
     *
     * In some cases, you need to set it to false, such as
     * <ul>
     *  <li>Using database/table sharding technology, such as sharding-jdbc</li>
     *  <li>Using database that does not support foreign key, such as TiDB</li>
     * </ul>
     */
    fun setForeignKeyEnabledByDefault(enabled: Boolean) {
        javaBuilder.setForeignKeyEnabledByDefault(enabled)
    }

    fun addCustomizers(vararg customers: KCustomizer) {
        javaBuilder.addCustomizers(customers.map { JavaCustomizer(it) })
    }

    fun addCustomizers(customers: Collection<KCustomizer>) {
        javaBuilder.addCustomizers(customers.map { JavaCustomizer(it) })
    }

    fun addInitializers(vararg initializers: KInitializer) {
        javaBuilder.addInitializers(initializers.map { JavaInitializer(it) })
    }

    fun addInitializers(initializers: Collection<KInitializer>) {
        javaBuilder.addInitializers(initializers.map { JavaInitializer(it) })
    }

    fun setDatabaseValidationMode(mode: DatabaseValidationMode) {
        javaBuilder.setDatabaseValidationMode(mode)
    }

    fun setDatabaseValidationCatalog(catalog: String?) {
        javaBuilder.setDatabaseValidationCatalog(catalog)
    }

    /**
     * For RDBMS, pagination is slow if `offset` is large, especially for MySQL.
     *
     * If `offset` >= $thisArgument
     *
     * <pre>{@code
     *  select t.* from Table t ... limit ? offset ?
     * }</pre>
     *
     * will be automatically changed to
     *
     * <pre>{@code
     *  select t.* from (
     *      select
     *          t.id as optimized_core_id_
     *      from Table t ... limit ? offset ?
     *  ) optimized_core_
     *  inner join Table as optimized_
     *      on optimized_.optimized_core_id_ = optimized_core_.optimized_core_id_
     * }</pre>
     *
     * @return An integer which is greater than 0
     */
    fun setOffsetOptimizingThreshold(threshold: Int) {
        javaBuilder.setOffsetOptimizingThreshold(threshold)
    }

    fun setMicroServiceName(microServiceName: String) {
        javaBuilder.setMicroServiceName(microServiceName)
    }

    fun setMicroServiceExchange(exchange: MicroServiceExchange) {
        javaBuilder.setMicroServiceExchange(exchange)
    }

    @DslScope
    class ConnectionManagerDsl internal constructor(
        private val javaBlock: Function<Connection, *>
    ) {
        private var proceeded: Boolean = false

        private var result: Any? = null

        fun proceed(con: Connection) {
            if (proceeded) {
                throw IllegalStateException("ConnectionManagerDsl cannot be proceeded twice")
            }
            result = javaBlock.apply(con)
            proceeded = true
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <R> get(): R {
            if (!proceeded) {
                throw IllegalStateException("ConnectionManagerDsl has not be proceeded")
            }
            return result as R
        }
    }

    private class ConnectionManagerImpl(
        private val dslBlock: ConnectionManagerDsl.() -> Unit
    ) : ConnectionManager {

        override fun <R> execute(block: Function<Connection, R>): R =
            ConnectionManagerDsl(block).let {
                it.dslBlock()
                it.get()
            }
    }

    @DslScope
    class CacheDsl internal constructor(
        private val javaCfg: CacheConfig
    ) {
        fun setCacheFactory(cacheFactory: CacheFactory) {
            javaCfg.setCacheFactory(cacheFactory)
        }

        fun <T: Any> setObjectCache(entityType: KClass<T>, cache: Cache<*, T>?) {
            javaCfg.setObjectCache(entityType.java, cache)
        }

        fun setAssociatedIdCache(prop: KProperty1<*, *>, cache: Cache<*, *>?) {
            javaCfg.setAssociatedIdCache(prop.toImmutableProp(), cache)
        }

        fun setAssociatedListIdCache(prop: KProperty1<*, *>, cache: Cache<*, List<*>>?) {
            javaCfg.setAssociatedIdListCache(prop.toImmutableProp(), cache)
        }

        fun <R> setCalculatedCache(prop: KProperty1<*, R>, cache: Cache<*, R>) {
            javaCfg.setCalculatedCache(prop.toImmutableProp(), cache)
        }

        fun setAbandonedCallback(callback: CacheAbandonedCallback?) {
            javaCfg.setAbandonedCallback(callback);
        }
    }

    internal fun buildKSqlClient(): KSqlClient =
        KSqlClientImpl(javaBuilder.build() as JSqlClientImplementor)
}