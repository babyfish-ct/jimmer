package org.babyfish.jimmer.ddl.compiler

import java.io.File

object JimmerDdlCompilerFiles {
    private const val SNAPSHOT_DIR_NAME = ".jimmer-ddl"
    private const val SNAPSHOT_FILE_NAME = "entity-table-snapshot.properties"

    fun resolveOutputFile(settings: JimmerDdlCompilerSettings): File {
        val outputDir = File(settings.outputDir)
        return File(outputDir, settings.outputFileName)
    }

    fun resolveSnapshotFile(settings: JimmerDdlCompilerSettings): File? {
        val projectDir = settings.outputDir.toProjectDir() ?: return null
        return projectDir.resolve(SNAPSHOT_DIR_NAME).resolve(SNAPSHOT_FILE_NAME)
    }

    fun resolveGeneratedSnapshotFile(settings: JimmerDdlCompilerSettings): File {
        val resourcesDir = settings.outputDir.toGeneratedResourcesDir()
        return resourcesDir.resolve(SNAPSHOT_DIR_NAME).resolve(SNAPSHOT_FILE_NAME)
    }

    fun writeOutputFile(
        settings: JimmerDdlCompilerSettings,
        sql: String,
    ): File {
        val outputFile = resolveOutputFile(settings)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(sql)
        return outputFile
    }

    private fun String.toProjectDir(): File? {
        val absoluteOutputDir = File(this).absoluteFile.path
        return absoluteOutputDir
            .substringBefore("${File.separator}build${File.separator}", missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
            ?.let(::File)
    }

    private fun String.toGeneratedResourcesDir(): File {
        val outputDir = File(this).absoluteFile
        val parent = outputDir.parentFile
        if (outputDir.name == "migration" && parent?.name == "db") {
            return parent.parentFile ?: outputDir
        }
        return outputDir
    }
}
