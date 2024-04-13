package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.CompositeBookInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreNullableInput
import java.lang.AssertionError
import java.math.BigDecimal
import kotlin.test.*

class JacksonTest {

    @Test
    fun testFailed() {
        val json =
            """{
                |"edition":1,"price":79.9,
                |"store":{"name":"TURING","version":0,"website":null},
                |"authors":[
                |{"firstName":"Jim","lastName":"Green","gender":"MALE"},
                |{"firstName":"Kate","lastName":"White","gender":"FEMALE"}
                |]
                |}""".trimMargin().replace("\r", "").replace("\n", "")
        val ex = assertFails {
            jacksonObjectMapper().readValue(json, CompositeBookInput::class.java)
        }
        expect(
            """An object whose type is 
                |"org.babyfish.jimmer.sql.kt.model.classic.book.dto.CompositeBookInput" 
                |cannot be deserialized by Jackson. 
                |The current type is static input DTO so that all JSON properties must be specified explicitly, 
                |however, the property "id" is not specified by JSON explicitly. 
                |Please either explicitly specify the property as null in the JSON, 
                |or specify the current input type as dynamic in the DTO language""".trimMargin().replace("\n", "")
        ) {
            ex.cause!!.message
        }
    }

    @Test
    fun testSuccess() {
        val json =
            """{
                |"id":null,
                |"name":"SQL in Action","edition":1,"price":79.9,
                |"store":{"name":"TURING","version":0,"website":null},
                |"authors":[
                |{"firstName":"Jim","lastName":"Green","gender":"MALE"},
                |{"firstName":"Kate","lastName":"White","gender":"FEMALE"}
                |]
                |}""".trimMargin()
        val input = CompositeBookInput(
            name = "SQL in Action",
            edition = 1,
            price = BigDecimal("79.9"),
            store = CompositeBookInput.TargetOf_store(name = "TURING", version = 0),
            authors = listOf(
                CompositeBookInput.TargetOf_authors(
                    firstName = "Jim",
                    lastName = "Green",
                    gender = Gender.MALE
                ),
                CompositeBookInput.TargetOf_authors(
                    firstName = "Kate",
                    lastName = "White",
                    gender = Gender.FEMALE
                )
            )
        )
        val input2 = jacksonObjectMapper().readValue(json, CompositeBookInput::class.java)
        expect(input) {
            input2
        }
    }

    @Test
    fun testMissNonNull() {
        val ex = assertFails {
            jacksonObjectMapper().readValue(
                """{"name":"TURING"}""",
                BookStoreNullableInput::class.java
            )
        }
        expect(
            """An object whose type is 
                |"org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreNullableInput" 
                |cannot be deserialized by Jackson. 
                |The current type is static input DTO so that all JSON properties must be specified explicitly, 
                |however, the property "id" is not specified by JSON explicitly. 
                |Please either explicitly specify the property as null in the JSON, 
                |or specify the current input type as dynamic in the DTO language""".trimMargin().replace("\n", "")
        ) {
            ex.cause!!.message
        }
    }
}