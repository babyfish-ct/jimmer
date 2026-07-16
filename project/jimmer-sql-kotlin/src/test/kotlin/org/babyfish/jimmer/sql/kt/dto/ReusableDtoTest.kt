package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.classic.book.dto.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ReusableDtoTest {

    @Test
    fun testAssociationInputTypesAndReverseConversion() {
        val input = BookWithReusableAssociationsInput(
            id = 1L,
            name = "GraphQL in Action",
            store = ReusableBookStoreInput(
                id = 2L,
                name = "MANNING"
            ),
            authors = listOf(
                ReusableAuthorInput(
                    id = 3L,
                    firstName = "Samer",
                    lastName = "Buna"
                )
            )
        )

        val book = input.toImmutable()
        assertEquals("MANNING", book.store?.name)
        assertEquals("Samer", book.authors[0].firstName)
    }

    @Test
    fun testAssociationTypeAndReverseConversion() {
        val view = BookWithReusableStoreView(
            id = 1L,
            name = "GraphQL in Action",
            storeInfo = ReusableBookStoreView(
                id = 2L,
                name = "MANNING"
            )
        )

        assertEquals("MANNING", view.storeInfo?.name)
        assertEquals("MANNING", view.toImmutable().store?.name)
    }

    @Test
    fun testAssociationListTypeAndReverseConversion() {
        val view = BookWithReusableAuthorsView(
            id = 1L,
            name = "GraphQL in Action",
            authors = listOf(
                ReusableAuthorView(
                    id = 2L,
                    firstName = "Samer",
                    lastName = "Buna"
                )
            )
        )

        assertEquals("Samer", view.authors[0].firstName)
        assertEquals("Samer", view.toImmutable().authors[0].firstName)
    }
}
