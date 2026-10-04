package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.jackson.codec.ImmutableModuleCustomization
import org.babyfish.jimmer.jackson.codec.JsonCodec
import org.babyfish.jimmer.jackson.v2.JsonCodecProviderV2
import org.babyfish.jimmer.jackson.v3.JsonCodecProviderV3
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.Internal
import org.babyfish.jimmer.sql.cache.ValueSerializer
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.BookDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IdViewDraftIsolationTest {

    @Test
    fun testJackson2() {
        verify(JsonCodecProviderV2().create().withCustomizations(ImmutableModuleCustomization()))
    }

    @Test
    fun testJackson3() {
        verify(JsonCodecProviderV3().create().withCustomizations(ImmutableModuleCustomization()))
    }

    private fun verify(codec: JsonCodec<*>) {
        val serializer = ValueSerializer<Book>(ImmutableType.get(Book::class.java), codec)
        val bytes = serializer.serialize(BookDraft.Builder().id(1L).storeId(2L).authorIds(listOf(3L, 4L)).build())
        val cached = Internal.requiresNewDraftContext { serializer.deserialize(bytes) }
        val changed = Book(cached) { name = "Changed" }
        assertEquals(2L, changed.store!!.id)
        assertEquals(listOf(3L, 4L), changed.authors.map { it.id })
        assertFalse(cached.store is Draft)
        cached.authors.forEach { assertFalse(it is Draft) }
    }
}
