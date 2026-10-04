package org.babyfish.jimmer.ddl.compiler

import org.junit.AfterClass
import org.junit.BeforeClass

/**
 * Base for JUnit 4 tests that run the embedded KSP compiler, including parameterized tests.
 *
 * Its Kotlin 2.1 Java-version parser does not recognize JDK 25. Until the embedded compiler is
 * upgraded, expose a compatible version during each test class and restore it afterwards.
 * This does not change the running JDK or the compilation target supplied by `jimmer.test.jvmTarget`.
 */
abstract class AbstractKspTest {

    companion object {
        private var originalJavaVersion: String? = null

        @JvmStatic
        @BeforeClass
        fun setCompatibleJavaVersionForKsp() {
            originalJavaVersion = System.getProperty("java.version")
            System.setProperty("java.version", "21.0.0")
        }

        @JvmStatic
        @AfterClass
        fun restoreJavaVersion() {
            originalJavaVersion?.let { System.setProperty("java.version", it) }
        }
    }
}
