package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class JimmerProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return JimmerProcessor(environment)
    }
}