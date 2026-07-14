package org.babyfish.jimmer.ddl.compiler.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import org.babyfish.jimmer.ddl.compiler.JimmerDdlCompiler
import org.babyfish.jimmer.ddl.compiler.JimmerDdlEntityTableSnapshot
import org.babyfish.jimmer.ddl.compiler.JimmerDdlCompilerFiles
import org.babyfish.jimmer.ddl.compiler.JimmerDdlCompilerSettings
import org.babyfish.jimmer.ddl.compiler.toStableJimmerDdlSnapshot
import site.addzero.lsi.clazz.LsiClass
import site.addzero.lsi.ksp.clazz.toLsiClass
import java.io.File

private const val JIMMER_ENTITY = "org.babyfish.jimmer.sql.Entity"

class JimmerDdlCompilerProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val settingsList = JimmerDdlCompilerSettings.allFromOptions(environment.options)
        val projectDirs = settingsList.mapNotNull { settings ->
            settings.outputDir.toProjectDirFromOutputDir()
        }.toSet()
        return object : SymbolProcessor {
            private val entities = linkedMapOf<String, LsiClass>()
            private val relationTargetEntities = linkedMapOf<String, LsiClass>()
            private var hasErrors = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                if (settingsList.none { it.enabled }) {
                    return emptyList()
                }
                val deferred = mutableListOf<KSAnnotated>()
                resolver.getSymbolsWithAnnotation(JIMMER_ENTITY).forEach { symbol ->
                    if (!symbol.validate()) {
                        deferred += symbol
                        return@forEach
                    }
                    val declaration = symbol as? KSClassDeclaration
                    if (declaration == null) {
                        environment.logger.error("Jimmer DDL can only process class declarations: $symbol", symbol)
                        hasErrors = true
                        return@forEach
                    }
                    val key = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
                    val lsiClass = declaration.toLsiClass(resolver).toStableJimmerDdlSnapshot()
                    relationTargetEntities[key] = lsiClass
                    if (declaration.isCurrentProjectSource(projectDirs)) {
                        entities[key] = lsiClass
                    }
                }
                return deferred
            }

            override fun finish() {
                if (hasErrors || entities.isEmpty()) {
                    return
                }
                settingsList.forEach { settings ->
                    val result = JimmerDdlCompiler.compile(
                        classes = entities.values,
                        settings = settings,
                        relationTargetClasses = relationTargetEntities.values,
                    )
                    result.warnings.forEach { warning ->
                        environment.logger.warn(warning)
                    }
                    JimmerDdlEntityTableSnapshot.writeGeneratedSnapshot(
                        entities = result.entities,
                        schema = result.snapshotSchema,
                        settings = settings,
                    )
                    if (!result.isEmpty) {
                        val outputFile = JimmerDdlCompilerFiles.writeOutputFile(settings, result.sql)
                        environment.logger.warn("Jimmer DDL generated: ${outputFile.absolutePath}")
                    }
                }
            }
        }
    }
}

private fun KSClassDeclaration.isCurrentProjectSource(projectDirs: Set<String>): Boolean {
    val path = containingFile?.filePath?.normalizedPath() ?: return false
    if (path.contains("/build/generated/")) {
        return false
    }
    if (projectDirs.isEmpty()) {
        return true
    }
    return projectDirs.any { projectDir ->
        path.startsWith("$projectDir/src/")
    }
}

private fun String.toProjectDirFromOutputDir(): String? {
    val outputPath = File(this).absolutePath.normalizedPath()
    val index = outputPath.indexOf("/build/")
    if (index < 0) {
        return null
    }
    return outputPath.substring(0, index).trimEnd('/').takeIf { it.isNotBlank() }
}

private fun String.normalizedPath(): String {
    return replace('\\', '/')
}
