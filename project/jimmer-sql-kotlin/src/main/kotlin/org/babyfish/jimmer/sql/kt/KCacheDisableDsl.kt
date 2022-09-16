package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.CacheDisableConfig
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class KCacheDisableDsl internal constructor(
    private val javaCfg: CacheDisableConfig
) {
    fun disableAll() {
        javaCfg.disableAll()
    }

    fun disable(type: KClass<*>) {
        javaCfg.disable(type.java)
    }

    fun disable(type: ImmutableType) {
        javaCfg.disable(type)
    }

    fun disable(prop: KProperty1<*, *>) {
        javaCfg.disable(prop.toImmutableProp())
    }

    fun disable(prop: ImmutableProp) {
        javaCfg.disable(prop)
    }
}