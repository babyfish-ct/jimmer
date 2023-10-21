package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor

interface KSqlClientImplementor : KSqlClient {

    val loaders: KLoaders

    fun initializeByDIFramework()
}