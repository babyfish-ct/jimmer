package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.kt.model.inheritance.KBaseNode
import org.babyfish.jimmer.sql.kt.model.inheritance.KCable
import org.babyfish.jimmer.sql.kt.model.inheritance.KDevice
import org.babyfish.jimmer.sql.kt.model.inheritance.KPowerDevice
import org.babyfish.jimmer.sql.kt.model.inheritance.KThingModelDevice
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KPerson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class InheritanceMetadataTest {

    @Test
    fun testInheritanceChain() {
        val cable = KCable {
            id = 1L
            name = "Main cable"
            voltage = 10
            length = 100
        }
        val baseNodeType = ImmutableType.get(KBaseNode::class.java)
        val deviceType = ImmutableType.get(KDevice::class.java)
        val powerDeviceType = ImmutableType.get(KPowerDevice::class.java)
        val cableType = ImmutableType.get(KCable::class.java)

        assertEquals(1L, cable.id)
        assertEquals("Main cable", cable.name)
        assertEquals(10, cable.voltage)
        assertEquals(100, cable.length)
        assertSame(powerDeviceType, cableType.primarySuperType)
        assertSame(baseNodeType, cableType.inheritanceRoot)
        assertEquals(
            setOf(cableType, powerDeviceType, baseNodeType),
            cableType.allTypes
        )
        assertEquals(setOf(cableType), powerDeviceType.directDerivedTypes)
        assertEquals(setOf(deviceType, cableType), baseNodeType.inheritanceInfo!!.concreteTypes.toSet())
    }

    @Test
    fun testEntityAndMappedSuperclassMultipleInheritance() {
        val device = KDevice {
            id = 1L
            name = "Gateway"
            thingModelId = 2L
        }
        val baseNodeType = ImmutableType.get(KBaseNode::class.java)
        val deviceType = ImmutableType.get(KDevice::class.java)
        val thingModelDeviceType = ImmutableType.get(KThingModelDevice::class.java)

        assertEquals(1L, device.id)
        assertEquals("Gateway", device.name)
        assertEquals(2L, device.thingModelId)
        assertEquals(setOf(baseNodeType, thingModelDeviceType), deviceType.superTypes)
        assertSame(baseNodeType, deviceType.primarySuperType)
        assertSame(baseNodeType, deviceType.inheritanceRoot)
        assertSame(deviceType, deviceType.getProp("thingModelId").declaringType)
        assertEquals(
            setOf("id", "type", "name", "thingModelId"),
            deviceType.props.keys
        )
    }

    @Test
    fun testInheritanceInfo() {
        val clientType = ImmutableType.get(KClient::class.java)
        val organizationType = ImmutableType.get(KOrganization::class.java)
        val personType = ImmutableType.get(KPerson::class.java)

        val info = clientType.inheritanceInfo!!
        assertSame(clientType, clientType.inheritanceRoot)
        assertSame(clientType, organizationType.inheritanceRoot)
        assertSame(clientType, personType.inheritanceRoot)
        assertSame(info, organizationType.inheritanceInfo)
        assertSame(info, personType.inheritanceInfo)
        assertEquals(InheritanceType.JOINED, info.strategy)
        assertEquals("type", info.discriminatorProp.name)
        assertEquals("CLIENT_TYPE", info.discriminatorProp.getAnnotation(Column::class.java).name)

        assertNull(clientType.discriminatorValue)
        assertEquals("ORG", organizationType.discriminatorValue)
        assertEquals("KPerson", personType.discriminatorValue)
        assertEquals(
            "[KOrganization, KPerson]",
            info.concreteTypes.map { it.javaClass.simpleName }.toString()
        )
        assertEquals(
            "{ORG=org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KOrganization, " +
                    "KPerson=org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KPerson}",
            info.discriminatorTypeMap.toString()
        )
    }
}
