package org.babyfish.jimmer.sql.kt.cfg.impl

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KCustomizer
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.runtime.Customizer

internal class JavaCustomizer(
    private val customizer: KCustomizer
) : Customizer {

    override fun customize(builder: JSqlClient.Builder) {
        customizer.customize(KSqlClientDsl(builder))
    }
}