package org.babyfish.jimmer.sql.kt.cfg.impl

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KInitializer
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.babyfish.jimmer.sql.runtime.Initializer

internal class JavaInitializer(
    private val initializer: KInitializer
) : Initializer {

    override fun initialize(sqlClient: JSqlClient) {
        initializer.initialize(sqlClient.toKSqlClient())
    }
}