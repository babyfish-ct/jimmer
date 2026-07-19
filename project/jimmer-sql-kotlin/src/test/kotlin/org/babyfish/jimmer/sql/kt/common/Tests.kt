package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import kotlin.test.expect

fun assertContent(expected: String, actual: Any) {
    val normalizedExpected = expected.replace("--->", "").replace("\r", "").replace("\n", "")
    val actualString = actual.toString()

    // Try to parse as JSON and compare semantically to handle property ordering issues
    try {
        val expectedJson = jsonCodec().treeReader().read(normalizedExpected)
        val actualJson = jsonCodec().treeReader().read(actualString)
        expect(expectedJson) { actualJson }
    } catch (e: Exception) {
        // Fall back to string comparison if JSON parsing fails
        expect(normalizedExpected) { actualString }
    }
}