package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.dto.UserInput
import kotlin.test.Test
import kotlin.test.expect

class UserInputTest {

    @Test
    fun testUserDefinedDefaults() {
        val input = UserInput(1L, "Alex", tag3 = emptyList())

        expect(-7) {
            input.tag1
        }
        expect("Hello") {
            input.tag2
        }
    }
}
