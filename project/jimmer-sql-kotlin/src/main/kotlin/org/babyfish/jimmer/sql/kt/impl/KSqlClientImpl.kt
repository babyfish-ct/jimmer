package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.SqlClient
import org.babyfish.jimmer.sql.kt.KQueries
import org.babyfish.jimmer.sql.kt.KSqlClient
import kotlin.reflect.KClass

internal class KSqlClientImpl(
    private val sqlClient: SqlClient
) : KSqlClient {

    override val queries: KQueries = KQueriesImpl(sqlClient)
}