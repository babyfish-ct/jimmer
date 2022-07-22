package org.babyfish.jimmer.sql.kt

import kotlin.reflect.KClass

interface KSqlClient {

    fun createQuery(entityType: KClass<*>)

    companion object {

        fun create(block: KSqlClientDSL.() -> Unit): KSqlClient {
            val dsl = KSqlClientDSL()
            dsl.block()
            return dsl.buildKSqlClient()
        }
    }
}