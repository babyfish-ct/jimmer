package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.VersionMode
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandPartialDsl
import kotlin.test.Test
import kotlin.test.assertNotNull

class VersionModeApiShapeTest {

    @Test
    fun testVersionModeDslShape() {
        val block: KSaveCommandPartialDsl.() -> Unit = {
            setVersionMode(VersionMode.ASSIGNMENT)
        }
        assertNotNull(block)
    }
}
