package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.kt.model.Data
import org.babyfish.jimmer.kt.model.by
import kotlin.test.Test
import kotlin.test.expect

class DataTest {

    @Test
    fun testSerialize() {
        val data = new(Data::class).by {
            list = listOf(1L, 2L)
            nestedList = listOf(
                listOf(3L, 4L),
                listOf(5L, 6L)
            )
            arr = longArrayOf(7L, 8L)
            nestedArr = arrayOf(
                longArrayOf(9L, 10L),
                longArrayOf(11L, 2L)
            )
        }
        expect(
            """{"list":[1,2],"nestedList":[[3,4],[5,6]],"arr":[7,8],"nestedArr":[[9,10],[11,2]]}"""
        ) {
            data.toString()
        }
    }

    @Test
    fun testDeserialize() {
        val json = """{"list":[1,2],"nestedList":[[3,4],[5,6]],"arr":[7,8],"nestedArr":[[9,10],[11,2]]}"""
        val data = ImmutableObjects.fromString(Data::class.java, json)
        expect(json) {
            data.toString()
        }
    }
}