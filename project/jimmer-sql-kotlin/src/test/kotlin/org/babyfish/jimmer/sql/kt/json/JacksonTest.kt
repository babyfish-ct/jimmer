package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.json.codec.JsonCodec.defaultCodec
import org.babyfish.jimmer.sql.kt.model.inheritance.Administrator
import kotlin.test.Test
import kotlin.test.expect

class JacksonTest {

    @Test
    fun testIssue528() {
        val json = """{
            |"id": 1,
            |"deleted": true
            |}""".trimMargin()
        val administrator = defaultCodec()
            .readerFor(Administrator::class.java)
            .read(json)
        expect(
            """{"deleted":true,"id":1}"""
        ) {
            administrator.toString()
        }
    }
}
