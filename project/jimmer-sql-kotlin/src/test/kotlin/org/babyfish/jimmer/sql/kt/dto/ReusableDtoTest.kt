package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookWithReusableStoreView
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.ReusableBookStoreView
import kotlin.test.Test
import kotlin.test.assertEquals

class ReusableDtoTest {

    @Test
    fun testAssociationTypeAndReverseConversion() {
        val view = BookWithReusableStoreView(
            id = 1L,
            name = "GraphQL in Action",
            store = ReusableBookStoreView(
                id = 2L,
                name = "MANNING"
            )
        )

        assertEquals("MANNING", view.store?.name)
        assertEquals("MANNING", view.toImmutable().store?.name)
    }
}
