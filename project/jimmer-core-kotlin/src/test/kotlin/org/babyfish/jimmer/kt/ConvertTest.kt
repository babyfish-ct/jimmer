package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableConverter
import org.babyfish.jimmer.kt.model.*
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

class ConvertTest {

    @Test
    fun testPartialConverter() {
        val book = ImmutableConverter
            .forMethods(Book::class.java, Partial::class.java)
            .build()
            .convert(Partial("SQL in Action"))
        expect("{\"name\":\"SQL in Action\"}") {
            book.toString()
        }
    }

    @Test
    fun testCondConverter() {
        val book = ImmutableConverter
            .forMethods(Book::class.java, Partial::class.java)
            .map(Book::name) {
                useIf { name != null }
            }
            .build()
            .convert(Partial(null))
        expect("{}") {
            book.toString()
        }
    }

    @Test
    fun testDefaultConverter() {
        ImmutableConverter
            .forMethods(Book::class.java, BookInput::class.java)
            .unmapStaticProps(BookInput::storeName)
            .unmapStaticProps(BookInput::authorNames)
            .build()
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
        ImmutableConverter
            .forMethods(Book::class.java, BookInput::class.java)
            .map(Book::store, BookInput::storeName) {
                valueConverter {
                    new(BookStore::class).by {
                        name = it
                    }
                }
            }
            .mapList(Book::authors, BookInput::authorNames) {
                elementConverter {
                    new(Author::class).by {
                        val index = it.indexOf('-')
                        firstName = it.substring(0, index)
                        lastName = it.substring(index + 1)
                    }
                }
            }
            .build()
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
        ImmutableConverter
            .forMethods(Book::class.java, BookInput::class.java)
            .unmapStaticProps(BookInput::storeName)
            .unmapStaticProps(BookInput::authorNames)
            .setDraftModifier { draft, input ->
                (draft as BookDraft).store = input.storeName?.let {
                     new(BookStore::class).by {
                         name = it
                     }
                }
                for (authorName in input.authorNames) {
                    draft.authors().addBy {
                        val index = authorName.indexOf('-')
                        firstName = authorName.substring(0, index)
                        lastName = authorName.substring(index + 1)
                    }
                }
            }
            .build()
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
        val price: BigDecimal?,
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