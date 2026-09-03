package org.babyfish.jimmer.ksp.dto

import org.junit.BeforeClass

abstract class AbstractTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun setCompatibleJavaVersionForKsp() {
            System.setProperty("java.version", "21.0.0")
        }
    }
}