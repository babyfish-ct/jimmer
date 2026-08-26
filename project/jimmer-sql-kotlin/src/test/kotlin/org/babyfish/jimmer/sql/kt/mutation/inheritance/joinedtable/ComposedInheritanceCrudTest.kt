package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.KCable
import org.babyfish.jimmer.sql.kt.model.inheritance.KDevice
import org.babyfish.jimmer.sql.kt.model.inheritance.KPowerDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ComposedInheritanceCrudTest : AbstractMutationTest() {

    @Test
    fun testEntityAndMappedSuperclassMultipleInheritanceCrud() {
        jdbc(rollback = true) { con ->
            val entities = sqlClient.entities.forConnection(con)

            val insertCount = entities.save(
                KDevice {
                    id = 700L
                    name = "Gateway"
                    thingModelId = 800L
                }
            ) {
                setMode(SaveMode.INSERT_ONLY)
            }.totalAffectedRowCount
            assertEquals(2, insertCount)

            val inserted = entities.findOneById(KDevice::class, 700L)
            assertEquals("Gateway", inserted.name)
            assertEquals(800L, inserted.thingModelId)

            val updateCount = entities.save(
                KDevice {
                    id = 700L
                    name = "Gateway 2"
                    thingModelId = 801L
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }.totalAffectedRowCount
            assertEquals(2, updateCount)

            val updated = entities.findOneById(KDevice::class, 700L)
            assertEquals("Gateway 2", updated.name)
            assertEquals(801L, updated.thingModelId)

            val deleteCount = entities.delete(KDevice::class, 700L) {
                setMode(DeleteMode.PHYSICAL)
            }.totalAffectedRowCount
            assertEquals(1, deleteCount)
            assertNull(entities.findById(KDevice::class, 700L))
        }
    }

    @Test
    fun testInheritanceChainCrud() {
        jdbc(rollback = true) { con ->
            val entities = sqlClient.entities.forConnection(con)

            val insertCount = entities.save(
                KCable {
                    id = 701L
                    name = "Main cable"
                    length = 100
                }
            ) {
                setMode(SaveMode.INSERT_ONLY)
            }.totalAffectedRowCount
            assertEquals(2, insertCount)

            val inserted = entities.findOneById(KCable::class, 701L)
            assertEquals("Main cable", inserted.name)
            assertEquals(100, inserted.length)

            val insertedAsPowerDevice = entities.findOneById(KPowerDevice::class, 701L)
            assertIs<KCable>(insertedAsPowerDevice)

            val updateCount = entities.save(
                KCable {
                    id = 701L
                    name = "Main cable 2"
                    length = 120
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }.totalAffectedRowCount
            assertEquals(2, updateCount)

            val updated = entities.findOneById(KCable::class, 701L)
            assertEquals("Main cable 2", updated.name)
            assertEquals(120, updated.length)

            val deleteCount = entities.delete(KCable::class, 701L) {
                setMode(DeleteMode.PHYSICAL)
            }.totalAffectedRowCount
            assertEquals(1, deleteCount)
            assertNull(entities.findById(KCable::class, 701L))
        }
    }
}
