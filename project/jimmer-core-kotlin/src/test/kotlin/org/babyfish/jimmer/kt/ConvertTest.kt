package org.babyfish.jimmer.kt

import org.babyfish.jimmer.kt.model.*
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

class ConvertTest {

    @Test
    fun testFullConverter() {
        assertFailsWith(IllegalArgumentException::class) {
            newImmutableConverter(Book::class, Partial::class) {
                autoMapOtherScalars()
            }
        }.let {
            expect(
                "Cannot automatically map the property " +
                    "\"org.babyfish.jimmer.kt.model.Book.edition\", " +
                    "the following non-static methods cannot be found in static type " +
                    "\"org.babyfish.jimmer.kt.ConvertTest\$Partial\": getEdition(), edition()"
            ) {
                it.message
            }
        }
    }

    @Test
    fun testPartialConverter() {
        val book = newImmutableConverter(Book::class, Partial::class) {
            autoMapOtherScalars(true)
        }.convert(Partial("SQL in Action"))
        expect("{\"name\":\"SQL in Action\"}") {
            book.toString()
        }
    }

    @Test
    fun testCondConverter() {
        val book = newImmutableConverter(Book::class, Partial::class) {
            map(Book::name) {
                useIf { it.name != null }
            }
        }.convert(Partial(null))
        expect("{}") {
            book.toString()
        }
    }

    @Test
    fun testEmptyConverter() {
        newImmutableConverter(Book::class, BookInput::class) {}
            .convert(INPUT)
            .let {
                expect(
                    "{}"
                ) {
                    it.toString()
                }
            }
    }

    @Test
    fun testDefaultConverter() {
        newImmutableConverter(Book::class, BookInput::class) {
            autoMapOtherScalars()
        }
            .convert(INPUT)
            .let {
                expect(
                    """{"name":"SQL in Action","edition":1,"price":49}"""
                ) {
                    it.toString()
                }
            }
    }

    @Test
    fun testWithValueConverter() {
        newImmutableConverter(Book::class, BookInput::class) {
            map(Book::store, BookInput::storeName) {
                valueConverter {
                    new(BookStore::class).by {
                        name = it
                    }
                }
            }
            mapList(Book::authors, BookInput::authorNames) {
                elementConverter {
                    new(Author::class).by {
                        val index = it.indexOf('-')
                        firstName = it.substring(0, index)
                        lastName = it.substring(index + 1)
                    }
                }
            }
            autoMapOtherScalars()
        }
            .convert(INPUT)
            .let {
                expect(
                    """{
                        |--->"name":"SQL in Action",
                        |--->"edition":1,
                        |--->"price":49,
                        |--->"store":{"name":"MANNING"},
                        |--->"authors":[
                        |--->--->{"firstName":"Tom","lastName":"Scott"},
                        |--->--->{"firstName":"Jessica","lastName":"Linda"}
                        |--->]
                        |}""".trimMargin().toSimpleLine()
                ) {
                    it.toString()
                }
            }
    }

    @Test
    fun testWithDraftModifier() {
        newImmutableConverter(Book::class, BookInput::class) {
            setDraftModifier<BookDraft> { input ->
                store = input.storeName?.let {
                     new(BookStore::class).by {
                         name = it
                     }
                }
                for (authorName in input.authorNames) {
                    authors().addBy {
                        val index = authorName.indexOf('-')
                        firstName = authorName.substring(0, index)
                        lastName = authorName.substring(index + 1)
                    }
                }
            }
            autoMapOtherScalars()
        }
            .convert(INPUT)
            .let {
                expect(
                    """{
                        |--->"name":"SQL in Action",
                        |--->"edition":1,
                        |--->"price":49,
                        |--->"store":{"name":"MANNING"},
                        |--->"authors":[
                        |--->--->{"firstName":"Tom","lastName":"Scott"},
                        |--->--->{"firstName":"Jessica","lastName":"Linda"}
                        |--->]
                        |}""".trimMargin().toSimpleLine()
                ) {
                    it.toString()
                }
            }
    }
    
    data class BookInput(
        val name: String,
        val price: BigDecimal,
        val edition: Int,
        val storeName: String?,
        val authorNames: List<String>
    )

    data class Partial(
        val name: String?
    )
    
    companion object {
        
        private val INPUT = BookInput(
            name = "SQL in Action",
            price = BigDecimal(49),
            edition = 1,
            storeName = "MANNING",
            authorNames = listOf("Tom-Scott", "Jessica-Linda")
        )

        fun String.toSimpleLine(): String =
            replace("\r", "").replace("\n", "").replace("--->", "")
    }
}