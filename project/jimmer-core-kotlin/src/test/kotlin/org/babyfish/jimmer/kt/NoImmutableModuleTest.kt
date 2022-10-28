package org.babyfish.jimmer.kt

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException
import org.babyfish.jimmer.kt.model.Book
import org.babyfish.jimmer.kt.model.by
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

class NoImmutableModuleTest {

    @Test
    fun test() {
        assertFailsWith(Throwable::class) {
            ObjectMapper().writeValueAsString(
                new(Book::class).by {}
            )
        }.cause.let {
            expect(ImmutableModuleRequiredException::class) {
                it!!::class
            }
        }
    }
}