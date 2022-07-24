package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.CacheFactory
import org.babyfish.jimmer.sql.cache.CachesImpl
import org.babyfish.jimmer.sql.dialect.DefaultDialect
import org.babyfish.jimmer.sql.dialect.Dialect
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl
import org.babyfish.jimmer.sql.kt.util.immutableProp
import org.babyfish.jimmer.sql.meta.IdGenerator
import org.babyfish.jimmer.sql.runtime.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class KSqlClientDSL internal constructor() {

    private val idGeneratorMap = mutableMapOf<Class<*>?, IdGenerator>()

    private val objectCacheMap = mutableMapOf<ImmutableType, Cache<*, *>>()

    private val associatedIdCacheMap = mutableMapOf<ImmutableProp, Cache<*, *>>()

    private val associatedIdListCacheMap = mutableMapOf<ImmutableProp, Cache<*, List<*>>>()

    private val scalarProviderMap = mutableMapOf<Class<*>?, ScalarProvider<*, *>?>()

    fun <T: Any> type(type: KClass<T>, block: TypeDSL<T>.() -> Unit) {
        TypeDSL(type).block()
    }

    fun scalarProviders(block: ScalarProviderDSL.() -> Unit) {
        ScalarProviderDSL().block()
    }

    var connectionManager: ConnectionManager? = null

    var dialect: Dialect = DefaultDialect()

    var executor: Executor = DefaultExecutor()

    var defaultBatchSize: Int = 128

    var defaultListBatchSize: Int = 16

    var idGenerator: IdGenerator?
        get() = idGeneratorMap[null]
        set(value) {
            if (value !== null) {
                idGeneratorMap[null] = value
            } else {
                idGeneratorMap.remove(null)
            }
        }

    var cacheFactory: CacheFactory? = null

    inner class ScalarProviderDSL internal constructor() {

        fun <T: Any> add(scalarProvider: ScalarProvider<T, *>) {
            scalarProviderMap[scalarProvider.scalarType] = scalarProvider
        }
    }

    inner class TypeDSL<T: Any> internal constructor(
        private val type: KClass<T>
    ) {
        fun reference(prop: KProperty1<T, *>, block: ReferencePropDSL.() -> Unit) {
            val immutableProp = immutableProp(prop)
            if (!immutableProp.isReference) {
                throw IllegalArgumentException("$prop is not reference property")
            }
            ReferencePropDSL(immutableProp).block()
        }

        fun list(prop: KProperty1<T, List<*>>, block: ListPropDSL.() -> Unit) {
            val immutableProp = immutableProp(prop)
            if (!immutableProp.isEntityList) {
                throw IllegalArgumentException("$prop is not list property")
            }
            ListPropDSL(immutableProp).block()
        }

        var idGenerator: IdGenerator?
            get() = idGeneratorMap[type.java]
            set(value) {
                if (value !== null) {
                    idGeneratorMap[type.java] = value
                } else {
                    idGeneratorMap.remove(type.java)
                }
            }

        @Suppress("UNCHECKED_CAST")
        var objectCache: Cache<*, T>?
            get() =
                objectCacheMap[ImmutableType.get(type.java)] as Cache<*, T>
            set(value) {
                if (value !== null) {
                    objectCacheMap[ImmutableType.get(type.java)] = value
                } else {
                    objectCacheMap.remove(ImmutableType.get(type.java))
                }
            }
    }

    inner class ReferencePropDSL internal constructor(private val prop: ImmutableProp) {
        var associatedCache: Cache<*, *>?
            get() = associatedIdCacheMap[prop]
            set(value) {
                if (value !== null) {
                    associatedIdCacheMap[prop] = value
                } else {
                    associatedIdCacheMap.remove(prop)
                }
            }
    }

    inner class ListPropDSL internal constructor(private val prop: ImmutableProp) {
        var associatedCache: Cache<*, List<*>>?
            get() = associatedIdListCacheMap[prop]
            set(value) {
                if (value !== null) {
                    associatedIdListCacheMap[prop] = value
                } else {
                    associatedIdListCacheMap.remove(prop)
                }
            }
    }

    internal fun buildKSqlClient(): KSqlClient =
        SqlClientImpl(
            connectionManager,
            dialect,
            executor,
            scalarProviderMap,
            idGeneratorMap,
            defaultBatchSize,
            defaultListBatchSize,
            CachesImpl(
                cacheFactory,
                objectCacheMap,
                associatedIdCacheMap,
                associatedIdListCacheMap
            )
        ).let {
            KSqlClientImpl(it)
        }
}