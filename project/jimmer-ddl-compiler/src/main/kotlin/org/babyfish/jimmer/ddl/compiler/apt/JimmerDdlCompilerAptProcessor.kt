package org.babyfish.jimmer.ddl.compiler.apt

import org.babyfish.jimmer.ddl.compiler.JimmerDdlCompiler
import org.babyfish.jimmer.ddl.compiler.JimmerDdlEntityTableSnapshot
import org.babyfish.jimmer.ddl.compiler.JimmerDdlCompilerFiles
import org.babyfish.jimmer.ddl.compiler.JimmerDdlCompilerSettings
import site.addzero.lsi.apt.clazz.toLsiClass
import site.addzero.lsi.clazz.LsiClass
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

private const val JIMMER_ENTITY = "org.babyfish.jimmer.sql.Entity"

@SupportedAnnotationTypes(JIMMER_ENTITY)
@SupportedOptions(
    "jimmerDdl.enabled",
    "jimmerDdl.profiles",
    "jimmerDdl.databaseType",
    "jimmerDdl.jdbcUrl",
    "jimmerDdl.jdbcUsername",
    "jimmerDdl.jdbcPassword",
    "jimmerDdl.jdbcSchema",
    "jimmerDdl.jdbcDriver",
    "jimmerDdl.springResourcePath",
    "jimmerDdl.springProfile",
    "jimmerDdl.outputFormat",
    "jimmerDdl.outputDir",
    "jimmerDdl.version",
    "jimmerDdl.description",
    "jimmerDdl.includePackages",
    "jimmerDdl.excludePackages",
    "jimmerDdl.includeForeignKeys",
    "jimmerDdl.includeIndexes",
    "jimmerDdl.includeComments",
    "jimmerDdl.includeSequences",
    "jimmerDdl.includeManyToManyTables",
    "jimmerDdl.compareDatabase",
    "jimmerDdl.nullabilityRepairOnly",
    "jimmerDdl.sourceFingerprint",
)
class JimmerDdlCompilerAptProcessor : AbstractProcessor() {
    private val entities = linkedMapOf<String, LsiClass>()
    private val supportedOptionNames = buildSet {
        addAll(
            listOf(
                "jimmerDdl.enabled",
                "jimmerDdl.profiles",
                "jimmerDdl.databaseType",
                "jimmerDdl.jdbcUrl",
                "jimmerDdl.jdbcUsername",
                "jimmerDdl.jdbcPassword",
                "jimmerDdl.jdbcSchema",
                "jimmerDdl.jdbcDriver",
                "jimmerDdl.springResourcePath",
                "jimmerDdl.springProfile",
                "jimmerDdl.outputFormat",
                "jimmerDdl.outputDir",
                "jimmerDdl.version",
                "jimmerDdl.description",
                "jimmerDdl.includePackages",
                "jimmerDdl.excludePackages",
                "jimmerDdl.includeForeignKeys",
                "jimmerDdl.includeIndexes",
                "jimmerDdl.includeComments",
                "jimmerDdl.includeSequences",
                "jimmerDdl.includeManyToManyTables",
                "jimmerDdl.compareDatabase",
                "jimmerDdl.nullabilityRepairOnly",
                "jimmerDdl.sourceFingerprint",
            )
        )
    }

    override fun getSupportedOptions(): MutableSet<String> {
        return supportedOptionNames.toMutableSet()
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(
        annotations: Set<TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        val settingsList = JimmerDdlCompilerSettings.allFromOptions(processingEnv.options)
        if (settingsList.none { it.enabled }) {
            return false
        }

        if (!roundEnv.processingOver()) {
            roundEnv.getElementsAnnotatedWithAny(annotations).forEach { entity ->
                val key = entity.qualifiedName.toString()
                entities[key] = entity.toLsiClass(processingEnv.elementUtils)
            }
            return false
        }

        if (entities.isEmpty()) {
            return false
        }
        settingsList.forEach { settings ->
            val result = JimmerDdlCompiler.compile(entities.values, settings)
            result.warnings.forEach { warning ->
                processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, warning)
            }
            JimmerDdlEntityTableSnapshot.writeGeneratedSnapshot(
                entities = result.entities,
                schema = result.snapshotSchema,
                settings = settings,
            )
            if (!result.isEmpty) {
                val outputFile = JimmerDdlCompilerFiles.writeOutputFile(settings, result.sql)
                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Jimmer DDL generated: ${outputFile.absolutePath}")
            }
        }
        return false
    }

    private fun RoundEnvironment.getElementsAnnotatedWithAny(annotations: Set<TypeElement>): List<TypeElement> {
        return annotations
            .flatMap { getElementsAnnotatedWith(it) }
            .filterIsInstance<TypeElement>()
    }
}
