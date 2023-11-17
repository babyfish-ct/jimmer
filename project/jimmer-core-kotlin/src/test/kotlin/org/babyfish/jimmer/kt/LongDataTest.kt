package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.kt.model.LongData
import org.babyfish.jimmer.kt.model.by
import kotlin.test.Test
import kotlin.test.expect

class LongDataTest {

    @Test
    fun test() {
        val data = new(LongData::class).by {
            nonNullValue = 1L
            nullableValue = 2L
            values = listOf(1L, 2L)
        }
        val json = data.toString()
        expect(
            """{"nonNullValue":"1","nullableValue":"2","values":["1","2"]}"""
        ) {
            json
        }
        val data2 = ImmutableObjects.fromString(LongData::class.java, json)
        expect(data) {
            data2
        }
    }
}