package org.babyfish.jimmer.sql.kt.cfg

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.impl.JavaInitializer
import org.babyfish.jimmer.sql.runtime.Initializer

fun interface KInitializer {

    fun initialize(dsl: KSqlClient): Unit
}

fun KInitializer.toJavaInitializer(): Initializer =
    JavaInitializer(this)