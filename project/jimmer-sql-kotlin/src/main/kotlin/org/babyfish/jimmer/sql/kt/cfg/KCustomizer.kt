package org.babyfish.jimmer.sql.kt.cfg

import org.babyfish.jimmer.sql.kt.cfg.impl.JavaCustomizer
import org.babyfish.jimmer.sql.runtime.Customizer

fun interface KCustomizer {

    fun customize(dsl: KSqlClientDsl): Unit
}

fun KCustomizer.toJavaCustomizer(): Customizer =
    JavaCustomizer(this)