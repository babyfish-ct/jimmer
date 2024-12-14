package org.babyfish.jimmer.sql.kt.model.ld.validation

import org.babyfish.jimmer.meta.ImmutableType
import kotlin.test.Test
import kotlin.test.expect

class ValidationTest {

    @Test
    fun testA() {
        val type = ImmutableType.get(A::class.java)
        val prop = type.getProp("deleted")
        val info = type.logicalDeletedInfo ?: error("Impossible")
        expect(false) {
            prop.defaultValueRef.value
        }
        expect(false) {
            info.allocateInitializedValue()
        }
        expect(true) {
            info.generateValue()
        }
    }

    @Test
    fun testB() {
        val type = ImmutableType.get(B::class.java)
        val prop = type.getProp("active")
        val info = type.logicalDeletedInfo ?: error("Impossible")
        expect(true) {
            prop.defaultValueRef.value
        }
        expect(true) {
            info.allocateInitializedValue()
        }
        expect(false) {
            info.generateValue()
        }
    }

    @Test
    fun testC() {
        val type = ImmutableType.get(C::class.java)
        val prop = type.getProp("state")
        val info = type.logicalDeletedInfo ?: error("Impossible")
        expect(0) {
            prop.defaultValueRef.value
        }
        expect(0) {
            info.allocateInitializedValue()
        }
        expect(2) {
            info.generateValue()
        }
    }

    @Test
    fun testD() {
        val type = ImmutableType.get(D::class.java)
        val prop = type.getProp("state")
        val info = type.logicalDeletedInfo ?: error("Impossible")
        expect(1) {
            prop.defaultValueRef.value
        }
        expect(1) {
            info.allocateInitializedValue()
        }
        expect(2) {
            info.generateValue()
        }
    }

    @Test
    fun testE() {
        val type = ImmutableType.get(E::class.java)
        val prop = type.getProp("state")
        val info = type.logicalDeletedInfo ?: error("Impossible")
        expect(State.NEW) {
            prop.defaultValueRef.value
        }
        expect(State.NEW) {
            info.allocateInitializedValue()
        }
        expect(State.DELETED) {
            info.generateValue()
        }
    }
}