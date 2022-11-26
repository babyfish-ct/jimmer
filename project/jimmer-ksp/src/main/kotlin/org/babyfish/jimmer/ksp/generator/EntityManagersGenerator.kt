package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import java.io.OutputStreamWriter

class EntityManagersGenerator(
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
            packageName,
            "EntityManagers"
        ).use {
            val fileSpec = FileSpec
                .builder(
                    packageName,
                    "EntityManagers"
                ).apply {
                    indent("    ")
                    addProperty(
                        PropertySpec
                            .builder("ENTITY_MANAGER", ENTITY_MANAGER_CLASS_NAME)
                            .initializer(
                                CodeBlock
                                    .builder()
                                    .apply {
                                        add("%T(\n", ENTITY_MANAGER_CLASS_NAME)
                                        indent()
                                        for (i in list.indices) {
                                            add(
                                                if (i + 1 == list.size) "%T::class.java\n" else "%T::class.java,\n",
                                                ClassName(list[i].packageName.asString(), list[i].simpleName.asString())
                                            )
                                        }
                                        unindent()
                                        add(")")
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