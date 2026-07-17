package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.sql.kt.model.classic.author.dto.ReusableAuthorInput
import org.babyfish.jimmer.sql.kt.model.classic.author.dto.ReusableAuthorView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookWithReusableAssociationsInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookWithReusableAuthorsView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookWithReusableStoreView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookWithReusableStoreInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.ReusableBookStoreInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.ReusableBookStoreView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReusableDtoTest {

    @Test
    fun testAssociationInputTypesAndReverseConversion() {
        val input = BookWithReusableAssociationsInput(
            id = 1L,
            name = "GraphQL in Action",
            store = ReusableBookStoreInput(
                id = 2L,
                name = "MANNING",
                label = "store"
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
        assertEquals("store", input.store?.label)
        assertEquals(
            "store",
            ReusableBookStoreInput(
                ReusableBookStoreView(
                    id = 2L,
                    name = "MANNING"
                ).toImmutable()
            ).label
        )
        assertEquals("MANNING", book.store?.name)
        assertEquals("Samer", book.authors[0].firstName)
    }

    @Test
    fun testDynamicAssociationInputPresence() {
        val emptyBook = DynamicBookWithReusableStoreInput().toImmutable()
        assertFalse(ImmutableObjects.isLoaded(emptyBook, "store"))

        val book = DynamicBookWithReusableStoreInput(
            store = ReusableBookStoreInput(
                id = 2L,
                name = "MANNING",
                label = "store"
            )
        ).toImmutable()
        assertTrue(ImmutableObjects.isLoaded(book, "store"))
        assertEquals("MANNING", book.store?.name)
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
