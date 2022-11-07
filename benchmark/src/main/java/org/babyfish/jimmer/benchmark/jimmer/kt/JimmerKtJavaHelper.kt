package org.babyfish.jimmer.benchmark.jimmer.kt

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.query.KTypedRootQuery

fun createKtQuery(sqlClient: KSqlClient): KTypedRootQuery<JimmerKtData> =
    sqlClient.createQuery(JimmerKtData::class) {
        select(table)
    }