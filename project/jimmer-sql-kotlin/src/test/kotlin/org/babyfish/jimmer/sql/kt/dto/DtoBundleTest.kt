package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BundledBookView
import org.junit.Test
import kotlin.test.assertEquals

class DtoBundleTest {

    @Test
    fun testBundledDtoIsGenerated() {
        assertEquals("BundledBookView", BundledBookView::class.simpleName)
    }
}
