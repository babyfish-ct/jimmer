package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.kt.model.AssociationInput
import org.babyfish.jimmer.kt.model.by
import kotlin.test.Test
import kotlin.test.expect

class AssociationInputTest {

    @Test
    fun test() {
        val input = new (AssociationInput::class).by {
            parentId = 3L
            childIds() += 10L
            childIds() += 11L
        }
        val input2 = ImmutableObjects.fromString(AssociationInput::class.java, input.toString())
        val input3 = new (AssociationInput::class).by(input) {
            parentId++
            val itr = childIds().listIterator()
            while (itr.hasNext()) {
                itr.set(itr.next() + 1)
            }
        }
        expect("""{"parentId":3,"childIds":[10,11]}""") { input.toString() }
        expect("""{"parentId":3,"childIds":[10,11]}""") { input2.toString() }
        expect("""{"parentId":4,"childIds":[11,12]}""") { input3.toString() }
    }
}