package org.babyfish.jimmer.kt

import org.babyfish.jimmer.kt.model.Complex
import org.babyfish.jimmer.kt.model.by
import kotlin.test.Test
import kotlin.test.expect

class ComplexTest {

    @Test
    fun test() {
        val complex = new(Complex::class).by {
            real = 5.0
            image = 7.0
        }
        val newComplex = new(Complex::class).by(complex) {
            real--
            image--
        }
        expect("""{"real":5.0,"image":7.0}""") { complex.toString() }
        expect("""{"real":4.0,"image":6.0}""") { newComplex.toString() }
    }
}