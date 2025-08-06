package org.babyfish.jimmer.ksp.jdbc2entity

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * JDBC 转 Jimmer 实体处理器提供者
 *
 * 用于创建 Jdbc2EntityProcessor 实例
 */
class Jdbc2EntityProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Jdbc2EntityProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options
        )
    }
}
