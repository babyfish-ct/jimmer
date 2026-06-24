package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.kt.model.mapsid.KMapsIdMessageDelivery
import org.babyfish.jimmer.sql.kt.model.mapsid.KMapsIdMessageDeliveryIdBase
import org.babyfish.jimmer.sql.kt.model.mapsid.KMapsIdMessageDeliveryMessageBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapsIdMetadataTest {

    @Test
    fun testWholeScalarIdMappingWithAssociationIdName() {
        val type = ImmutableType.get(KMapsIdMessageDelivery::class.java)
        val mappedIds = type.mappedIds

        assertEquals("messageId", type.idProp.name)
        assertNull(type.getProp("messageId").idViewBaseProp)
        assertNotNull(ImmutableType.get(KMapsIdMessageDeliveryIdBase::class.java).getProp("messageId"))
        assertNotNull(ImmutableType.get(KMapsIdMessageDeliveryMessageBase::class.java).getProp("message"))
        assertEquals(1, mappedIds.size)
        val mappedId = mappedIds[0]
        assertEquals("message", mappedId.prop.name)
        assertEquals(KMapsIdMessageDelivery::class.java, mappedId.prop.declaringType.javaClass)
        assertEquals("messageId", mappedId.idProp.name)
        assertEquals(KMapsIdMessageDelivery::class.java, mappedId.idProp.declaringType.javaClass)
        assertEquals("id", mappedId.targetIdProp.name)
        assertTrue(mappedId.isFull)
    }
}
