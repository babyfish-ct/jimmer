package org.babyfish.jimmer.ksp.immutable.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.ksp.util.guessResourceFile
import java.io.FileReader
import java.io.OutputStreamWriter
import java.nio.file.Files

class JimmerModuleGenerator(
    private val codeGenerator: CodeGenerator,
    private val packageName: String,
    private val declarations: List<KSDeclaration>,
    private val isModuleRequired: Boolean
) {
    fun generate(allFiles: List<KSFile>) {
        val list = declarations
        if (list.isEmpty()) {
            return
        }
        val guessedFile = guessResourceFile(codeGenerator.generatedFile.firstOrNull(), "entities")
        val qualifiedNames = sortedSetOf<String>()
        if (guessedFile != null && guessedFile.exists()) {
            FileReader(guessedFile).use {
                qualifiedNames += it.readLines()
            }
        }
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            "META-INF.jimmer",
            "entities",
            ""
        ).use {
            qualifiedNames += declarations
                .map { d -> d.qualifiedName!!.asString() }
            OutputStreamWriter(it).apply {
                for (qualifiedName in qualifiedNames) {
                    write(qualifiedName)
                    write("\n")
                }
                flush()
            }
        }
        if (!isModuleRequired) {
            return
        }
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            packageName,
            JIMMER_MODULE
        ).use {
            val fileSpec = FileSpec
                .builder(
                    packageName,
                    JIMMER_MODULE
                ).apply {
                    indent("    ")
                    addType(
                        TypeSpec
                            .classBuilder("JimmerModule")
                            .addModifiers(KModifier.PRIVATE)
                            .build()
                    )
                    addProperty(
                        PropertySpec
                            .builder("ENTITY_MANAGER", ENTITY_MANAGER_CLASS_NAME)
                            .addKdoc(
                                "Under normal circumstances, users do not need to use this code. \n" +
                                    "This code is for compatibility with version 0.7.47 and earlier."
                            )
                            .initializer(
                                CodeBlock
                                    .builder()
                                    .apply {
                                        add("%T.fromResources(\n", ENTITY_MANAGER_CLASS_NAME)
                                        indent()
                                        add("JimmerModule::class.java.classLoader\n")
                                        unindent()
                                        add(")")
                                        if (packageName.isNotEmpty()) {
                                            beginControlFlow("")
                                            add("it.name.startsWith(%S)\n", "$packageName.")
                                            endControlFlow()
                                        }
                                    }
                                    .build()
                            )
                            .build()
                    )
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }
}
