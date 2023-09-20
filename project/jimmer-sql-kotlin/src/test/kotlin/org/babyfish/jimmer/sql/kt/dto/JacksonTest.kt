package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.CompositeBookInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreNullableInput
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
        val ex = assertFailsWith<ValueInstantiationException>() {
            ObjectMapper().readValue(json, CompositeBookInput::class.java)
        }
        assertTrue { ex.message!!.contains("parameter name") }
    }

    @Test
    fun testSuccess() {
        val json =
            """{
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
        val input2 = ObjectMapper().readValue(json, CompositeBookInput::class.java)
        expect(input) {
            input2
        }
    }

    @Test
    fun testMissNonNull() {
        val ex = assertFails {
            ObjectMapper().readValue(
                """{"name": "TURING"}""",
                BookStoreNullableInput::class.java
            )
        }
        assertTrue {
            ex.message!!.contains("Missing required creator property 'version'")
        }
    }
}