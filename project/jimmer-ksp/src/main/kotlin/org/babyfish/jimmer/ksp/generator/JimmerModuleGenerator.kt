package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import java.io.OutputStreamWriter
import java.lang.IllegalArgumentException

class JimmerModuleGenerator(
    private val codeGenerator: CodeGenerator,
    private val packageName: String,
    private val declarations: List<KSDeclaration>
) {

    fun generate(allFiles: List<KSFile>) {
        val list = declarations
        if (list.isEmpty()) {
            return
        }
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            "META-INF.jimmer",
            "entities",
            ""
        ).use {
            val qualifiedNames = declarations
                .map { it.qualifiedName!!.asString() }
                .sorted()
                .toSet()
            OutputStreamWriter(it).apply {
                for (qualifiedName in qualifiedNames) {
                    write(qualifiedName)
                    write("\n")
                }
                flush()
            }
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
