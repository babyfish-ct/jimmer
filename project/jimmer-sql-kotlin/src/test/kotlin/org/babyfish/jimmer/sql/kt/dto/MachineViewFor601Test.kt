package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.embedded.Machine
import org.babyfish.jimmer.sql.kt.model.embedded.dto.MachineViewFor601
import org.junit.Test
import kotlin.test.expect

class MachineViewFor601Test {

    @Test
    fun testMachineViewFor601() {
        val machine = Machine {
            id = 1L
            detail {
                factories = mapOf(
                    "f-1" to "factory-1",
                    "f-2" to "factory-2"
                )
            }
        }
        expect(
            "MachineViewFor601(id=1, factoryNames=[f-1, f-2])"
        ) {
            MachineViewFor601(machine).toString()
        }
    }
}