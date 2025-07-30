package org.babyfish.jimmer.sql.kt.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.jackson.ImmutableModule
import kotlin.test.expect

private val MAPPER = ObjectMapper()
  .registerModule(ImmutableModule())
  .registerModule(JavaTimeModule())

fun assertContent(expected: String, actual: Any) {
  val normalizedExpected = expected.replace("--->", "").replace("\r", "").replace("\n", "")
  val actualString = actual.toString()

  // Try to parse as JSON and compare semantically to handle property ordering issues
  try {
    val expectedJson = MAPPER.readTree(normalizedExpected)
    val actualJson = MAPPER.readTree(actualString)
    expect(expectedJson) { actualJson }
  } catch (e: Exception) {
    // Fall back to string comparison if JSON parsing fails
    expect(normalizedExpected) { actualString }
    }
}