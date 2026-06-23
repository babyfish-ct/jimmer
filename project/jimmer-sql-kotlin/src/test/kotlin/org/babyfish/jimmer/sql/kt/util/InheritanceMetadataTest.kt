package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.kt.model.inheritance3.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance3.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance3.KPerson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InheritanceMetadataTest {

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
        assertEquals("CLIENT_TYPE", info.discriminatorColumn!!.name)

        assertEquals("KClient", clientType.discriminatorValue)
        assertEquals("ORG", organizationType.discriminatorValue)
        assertEquals("KPerson", personType.discriminatorValue)
        assertEquals(
            "[KClient, KOrganization, KPerson]",
            info.concreteTypes.map { it.javaClass.simpleName }.toString()
        )
        assertEquals(
            "{KClient=org.babyfish.jimmer.sql.kt.model.inheritance3.KClient, " +
                    "ORG=org.babyfish.jimmer.sql.kt.model.inheritance3.KOrganization, " +
                    "KPerson=org.babyfish.jimmer.sql.kt.model.inheritance3.KPerson}",
            info.discriminatorTypeMap.toString()
        )
    }
}
