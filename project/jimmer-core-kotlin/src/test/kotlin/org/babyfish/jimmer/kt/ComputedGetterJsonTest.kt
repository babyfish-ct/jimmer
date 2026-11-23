package org.babyfish.jimmer.kt

import org.babyfish.jimmer.jackson.v2.JsonCodecV2
import org.babyfish.jimmer.jackson.v3.JsonCodecV3
import org.babyfish.jimmer.json.JsonCodec
import org.babyfish.jimmer.kt.model.Author
import kotlin.test.Test
import kotlin.test.assertEquals

class ComputedGetterJsonTest {

    @Test
    fun testJackson2() {
        verify(JsonCodecV2())
    }

    @Test
    fun testJackson3() {
        verify(JsonCodecV3())
    }

    private fun verify(codec: JsonCodec) {
        val partial = Author { firstName = "Jim" }
        assertEquals("""{"firstName":"Jim"}""", codec.writer().writeAsString(partial))
        assertEquals(partial, codec.readerFor(Author::class.java).read("""{"firstName":"Jim"}"""))

        val complete = Author {
            firstName = "Jim"
            lastName = "Miller"
            assertEquals("""{"firstName":"Jim","lastName":"Miller"}""", codec.writer().writeAsString(this))
        }
        assertEquals("Jim Miller", complete.fullName)
        assertEquals("""{"firstName":"Jim","lastName":"Miller"}""", codec.writer().writeAsString(complete))
    }
}
