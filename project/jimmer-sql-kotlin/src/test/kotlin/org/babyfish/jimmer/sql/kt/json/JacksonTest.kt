package org.babyfish.jimmer.sql.kt.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.jackson.ImmutableModule
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
        val administrator = jacksonObjectMapper()
            .registerModule(ImmutableModule())
            .readValue(json, Administrator::class.java)
        expect(
            """{"deleted":true,"id":1}"""
        ) {
            administrator.toString()
        }
    }
}