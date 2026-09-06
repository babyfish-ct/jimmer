package org.babyfish.jimmer.ksp.dto

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.babyfish.jimmer.ksp.JimmerProcessorProvider
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.BeforeClass

abstract class AbstractTest {

    @OptIn(ExperimentalCompilerApi::class)
    protected fun createCompilation(): KotlinCompilation = KotlinCompilation().apply {
        jvmTarget = requireNotNull(System.getProperty("jimmer.test.jvmTarget")) {
            "Missing jimmer.test.jvmTarget; run the KSP tests through Gradle"
        }
        useKsp2()
        symbolProcessorProviders = mutableListOf(JimmerProcessorProvider())
        inheritClassPath = true
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun setCompatibleJavaVersionForKsp() {
            System.setProperty("java.version", "21.0.0")
        }
    }
}
