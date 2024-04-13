package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.expect

class UnloadedPropTest {

    @Test
    fun testStaticInputWithFullValue() {
        val json =
            """{
                |    "id": 100,
                |    "name": "SQL in Action",
                |    "edition": 2,
                |    "price": 49.9,
                |    "storeId": 3,
                |    "authorIds": [11, 12]
                |}""".trimMargin()
        val mapper = jacksonObjectMapper()
        val input = mapper.readValue(json, BookInput::class.java)
        assertContent(
            "BookInput(" +
                "--->name=SQL in Action, " +
                "--->edition=2, " +
                "--->price=49.9, " +
                "--->id=100, " +
                "--->storeId=3, " +
                "--->authorIds=[11, 12]" +
                ")",
            input
        )
    }

    @Test
    fun testStaticInputWithNullValue() {
        val json =
            """{
                |    "id": 100,
                |    "name": "SQL in Action",
                |    "edition": 2,
                |    "price": 49.9,
                |    "storeId": null,
                |    "authorIds": [11, 12]
                |}""".trimMargin()
        val mapper = jacksonObjectMapper()
        val input = mapper.readValue(json, BookInput::class.java)
        assertContent(
            "BookInput(" +
                "--->name=SQL in Action, " +
                "--->edition=2, " +
                "--->price=49.9, " +
                "--->id=100, " +
                "--->storeId=null, " +
                "--->authorIds=[11, 12]" +
                ")",
            input
        )
    }

    @Test
    fun testStaticInputWithPartialValue() {
        val json =
            """{
                |    "id": 100,
                |    "name": "SQL in Action",
                |    "edition": 2,
                |    "price": 49.9,
                |    "authorIds": [11, 12]
                |}""".trimMargin()
        val mapper = jacksonObjectMapper()
        val ex = assertFails {
            mapper.readValue(json, BookInput::class.java)
        }
        assertContent(
            """An object whose type is "org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookInput" 
                |cannot be deserialized by Jackson. 
                |The current type is static input DTO so that all JSON properties must be specified explicitly, 
                |however, the property "storeId" is not specified by JSON explicitly. 
                |Please either explicitly specify the property as null in the JSON, 
                |or specify the current input type as dynamic in the DTO language""".trimMargin(),
            ex.cause!!.message!!
        )
    }

    @Test
    fun testDynamicInputWithFullValue() {
        val json =
            """{
                |    "name": "SQL in Action",
                |    "edition": 2,
                |    "price": 49.9,
                |    "storeId": 3,
                |    "authorIds": [11, 12]
                |}""".trimMargin()
        val mapper = jacksonObjectMapper()
        val input = mapper.readValue(json, DynamicBookInput::class.java)
        assertContent(
            "DynamicBookInput(" +
                "--->name=SQL in Action, " +
                "--->edition=2, " +
                "--->price=49.9, " +
                "--->storeId=3, " +
                "--->authorIds=[11, 12]" +
                ")",
            input
        )
        expect(true) { input.isStoreIdLoaded }
    }

    @Test
    fun testDynamicInputWithNullValue() {
        val json =
            """{
                |    "name": "SQL in Action",
                |    "edition": 2,
                |    "price": 49.9,
                |    "storeId": null,
                |    "authorIds": [11, 12]
                |}""".trimMargin()
        val mapper = jacksonObjectMapper()
        val input = mapper.readValue(json, DynamicBookInput::class.java)
        assertContent(
            "DynamicBookInput(" +
                "--->name=SQL in Action, " +
                "--->edition=2, " +
                "--->price=49.9, " +
                "--->storeId=null, " +
                "--->authorIds=[11, 12]" +
                ")",
            input
        )
        expect(true) { input.isStoreIdLoaded }
    }

    @Test
    fun testDynamicInputWithPartialValue() {
        val json =
            """{
                |    "name": "SQL in Action",
                |    "edition": 2,
                |    "price": 49.9,
                |    "authorIds": [11, 12]
                |}""".trimMargin()
        val mapper = jacksonObjectMapper()
        val input = mapper.readValue(json, DynamicBookInput::class.java)
        assertContent(
            "DynamicBookInput(" +
                "--->name=SQL in Action, " +
                "--->edition=2, " +
                "--->price=49.9, " +
                "--->storeId=null, " +
                "--->authorIds=[11, 12]" +
                ")",
            input
        )
        expect(false) { input.isStoreIdLoaded }
    }
}