package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookNullableIdInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookNullableInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreNonNullInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreNullableIdInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreNullableInput
import org.babyfish.jimmer.sql.kt.model.enumeration.Gender
import org.babyfish.jimmer.sql.kt.model.enumeration.dto.ArticleNullableIdInput
import org.babyfish.jimmer.sql.kt.model.enumeration.dto.ArticleNullableInput
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.expect

class NullityTest {

    @Test
    fun testStatic() {
        expect(true) {
            BookStoreNullableInput::website.returnType.isMarkedNullable
        }
        expect(false) {
            BookStoreNonNullInput::website.returnType.isMarkedNullable
        }
    }

    @Test
    fun testBookWithNonNullStore() {
        val input = BookNullableInput(
            name = "SQL in Action",
            edition = 1,
            price = BigDecimal.TEN,
            store = BookNullableInput.TargetOf_store(
                name = "TURING",
                version = 1
            )
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"SQL in Action",
                |--->"edition":1,
                |--->"price":10,
                |--->"store":{
                |--->--->"name":"TURING",
                |--->--->"version":1,
                |--->--->"website":null
                |--->}
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookNullableInput(
                |--->id=null, 
                |--->name=SQL in Action, 
                |--->edition=1, 
                |--->price=10, 
                |--->store=BookNullableInput.TargetOf_store(
                |--->--->id=null, 
                |--->--->name=TURING, 
                |--->--->version=1, 
                |--->--->website=null
                |--->)
                |)""".trimMargin(),
            BookNullableInput(entity)
        )
    }

    @Test
    fun testBookWithNullStore() {
        val input = BookNullableInput(
            name = "SQL in Action",
            edition = 1,
            price = BigDecimal.TEN
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"SQL in Action",
                |--->"edition":1,
                |--->"price":10,
                |--->"store":null
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookNullableInput(
                |--->id=null, 
                |--->name=SQL in Action, 
                |--->edition=1, 
                |--->price=10, 
                |--->store=null
                |)""".trimMargin(),
            BookNullableInput(entity)
        )
    }

    @Test
    fun testArticleWithNonNullStore() {
        val input = ArticleNullableInput(
            id = 1L,
            name = "Introduce Jimmer",
            writer = ArticleNullableInput.TargetOf_writer(
                id = 2L,
                name = "Bob",
                gender = Gender.MALE
            )
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"id":1,
                |--->"name":"Introduce Jimmer",
                |--->"writer":{
                |--->--->"id":2,
                |--->--->"name":"Bob",
                |--->--->"gender":"MALE"
                |--->}
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """ArticleNullableInput(
                |--->id=1, 
                |--->name=Introduce Jimmer, 
                |--->writer=ArticleNullableInput.TargetOf_writer(
                |--->--->id=2, 
                |--->--->name=Bob, 
                |--->--->gender=MALE
                |--->)
                |)""".trimMargin(),
            ArticleNullableInput(entity)
        )
    }

    @Test
    fun testArticleWithNullSource() {
        val input = ArticleNullableInput(
            id = 1L,
            name = "Introduce Jimmer"
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"id":1,
                |--->"name":"Introduce Jimmer"
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """ArticleNullableInput(id=1, name=Introduce Jimmer, writer=null)""",
            ArticleNullableInput(entity)
        )
    }

    @Test
    fun testBookStoreWithNonNullBooks() {
        val input = BookStoreNullableInput(
            name = "TURING",
            version = 1,
            books = emptyList()
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"TURING",
                |--->"version":1,
                |--->"website":null,
                |--->"books":[]
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookStoreNullableInput(
                |--->id=null, 
                |--->name=TURING, 
                |--->version=1, 
                |--->website=null, 
                |--->books=[]
                |)""".trimMargin(),
            BookStoreNullableInput(entity)
        )
    }

    @Test
    fun testBookStoreWithNullBooks() {
        val input = BookStoreNullableInput(
            name = "TURING",
            version = 1
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"TURING",
                |--->"version":1,
                |--->"website":null,
                |--->"books":[]
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookStoreNullableInput(
                |--->id=null, 
                |--->name=TURING, 
                |--->version=1, 
                |--->website=null, 
                |--->books=[]
                |)""".trimMargin(),
            BookStoreNullableInput(entity)
        )
    }

    //////////////////////////////////////////////////

    @Test
    fun testBookWithNonNullStoreId() {
        val input = BookNullableIdInput(
            name = "SQL in Action",
            edition = 1,
            price = BigDecimal.TEN,
            storeId = 2L
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"SQL in Action",
                |--->"edition":1,
                |--->"price":10,
                |--->"store":{
                |--->--->"id":2
                |--->}
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookNullableIdInput(
                |--->id=null, 
                |--->name=SQL in Action, 
                |--->edition=1, 
                |--->price=10, 
                |--->storeId=2
                |)""".trimMargin(),
            BookNullableIdInput(entity)
        )
    }

    @Test
    fun testBookWithNullStoreId() {
        val input = BookNullableIdInput(
            name = "SQL in Action",
            edition = 1,
            price = BigDecimal.TEN
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"SQL in Action",
                |--->"edition":1,
                |--->"price":10,
                |--->"store":null
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookNullableIdInput(
                |--->id=null, 
                |--->name=SQL in Action, 
                |--->edition=1, 
                |--->price=10, 
                |--->storeId=null
                |)""".trimMargin(),
            BookNullableIdInput(entity)
        )
    }

    @Test
    fun testArticleWithNonNullWriterId() {
        val input = ArticleNullableIdInput(
            id = 1L,
            name = "Introduce Jimmer",
            writerId = 2L
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"id":1,
                |--->"name":"Introduce Jimmer",
                |--->"writer":{
                |--->--->"id":2
                |--->}
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """ArticleNullableIdInput(
                |--->id=1, 
                |--->name=Introduce Jimmer, 
                |--->writerId=2
                |)""".trimMargin(),
            ArticleNullableIdInput(entity)
        )
    }

    @Test
    fun testArticleWithNullWriterId() {
        val input = ArticleNullableIdInput(
            id = 1L,
            name = "Introduce Jimmer"
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"id":1,
                |--->"name":"Introduce Jimmer"
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """ArticleNullableIdInput(
                |--->id=1, 
                |--->name=Introduce Jimmer, 
                |--->writerId=null
                |)""".trimMargin(),
            ArticleNullableIdInput(entity)
        )
    }

    @Test
    fun testBookStoreWithNonNullBookIds() {
        val input = BookStoreNullableIdInput(
            name = "TURING",
            version = 1,
            bookIds = emptyList()
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{
                |--->"name":"TURING",
                |--->"version":1,
                |--->"website":null,
                |--->"books":[]
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookStoreNullableIdInput(
                |--->id=null, 
                |--->name=TURING, 
                |--->version=1, 
                |--->website=null, 
                |--->bookIds=[]
                |)""".trimMargin(),
            BookStoreNullableIdInput(entity)
        )
    }

    @Test
    fun testBookStoreWithNullBookIds() {
        val input = BookStoreNullableIdInput(
            name = "TURING",
            version = 1
        )
        val entity = input.toEntity()
        assertContentEquals(
            """{"
                |--->name":"TURING",
                |--->"version":1,
                |--->"website":null,
                |--->"books":[]
                |}""".trimMargin(),
            entity
        )
        assertContentEquals(
            """BookStoreNullableIdInput(
                |--->id=null, 
                |--->name=TURING, 
                |--->version=1, 
                |--->website=null, 
                |--->bookIds=[]
                |)""".trimMargin(),
            BookStoreNullableIdInput(entity)
        )
    }
}