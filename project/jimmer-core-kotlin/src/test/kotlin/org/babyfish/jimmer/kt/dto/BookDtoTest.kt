package org.babyfish.jimmer.kt.dto

import org.junit.Test
import kotlin.test.expect

class BookDtoTest {

    @Test
    fun test() {
        val bookDto = BookDto(
            "SQL in Action",
            "1",
            "65",
            BookDto.TargetOfStore("TRUING"),
            listOf(BookDto.TargetOfAuthors("Jim", "Cook"), BookDto.TargetOfAuthors("Linda", "White"))
        )
        val book = bookDto.toEntity()
        assertContents(
            """{
                |--->"name":"SQL in Action",
                |--->"edition":1,
                |--->"price":65,
                |--->"store":{"name":"TRUING"},
                |--->"authors":[
                |--->--->{"firstName":"Jim","lastName":"Cook"},
                |--->--->{"firstName":"Linda","lastName":"White"}
                |--->]
                |}""".trimMargin(),
            book
        )
    }

    companion object {
        fun assertContents(content: String, o: Any) {
            expect(
                content
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("--->", "")
            ) {
                o.toString()
            }
        }
    }
}