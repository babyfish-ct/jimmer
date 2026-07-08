package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.spi.ImmutableTypeImplementor
import org.babyfish.jimmer.sql.kt.model.selfref.SelfReferenceNode
import kotlin.test.Test
import kotlin.test.assertEquals

class SelfReferenceMetadataTest {

    @Test
    fun testFakeUpdatePropForSelfReferenceWithIdView() {
        val type = ImmutableType.get(SelfReferenceNode::class.java)
        assertEquals("name", (type as ImmutableTypeImplementor).fakeUpdateProp?.name)
    }
}
