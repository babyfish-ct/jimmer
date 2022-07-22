package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.SqlClient
import org.babyfish.jimmer.sql.kt.KSqlClient
import kotlin.reflect.KClass

class KSqlClientImpl(
    private val sqlClient: SqlClient
) : KSqlClient {

    override fun createQuery(entityType: KClass<*>) {
        TODO("Not yet implemented")
    }
}