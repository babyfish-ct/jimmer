package org.babyfish.jimmer.ksp.immutable

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.babyfish.jimmer.ksp.dto.AbstractTest
import org.babyfish.jimmer.meta.ImmutableType
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class PlainGetterTest : AbstractTest() {

    @Test
    fun `plain getters execute their declared implementations`() {
        val result = createCompilation().apply {
            sources = listOf(SourceFile.kotlin("Named.kt", """
                package example
                import org.babyfish.jimmer.sql.*
                import org.babyfish.jimmer.kt.new

                @MappedSuperclass
                interface Named {
                    val name: String
                    val inheritedSimple: String get() = name.uppercase()
                    val inheritedComplex: Map<String, Int>
                        get() {
                            val words = name.split(' ').filter { it.isNotBlank() }
                            return words.associateWith { it.length }
                        }
                }

            """.trimIndent()), SourceFile.kotlin("Author.kt", """
                package example
                import org.babyfish.jimmer.sql.*
                import org.babyfish.jimmer.kt.new

                @Entity
                interface Author : Named {
                    @Id val id: Long
                    val simple: String get() = "Hello " + name
                    val complex: List<String>
                        get() {
                            val words = name.split(' ')
                            return words.filter { it.isNotBlank() }.reversed()
                        }
                }

                object AuthorFactory {
                    @JvmStatic
                    fun create(): Author = new(Author::class).by {
                        id = 1L
                        name = "Ada Lovelace"
                    }
                }
            """.trimIndent()))
        }.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val entityClass = result.classLoader.loadClass("example.Author")
        assertEquals(setOf("id", "name"), ImmutableType.get(entityClass).props.keys)
        val author = result.classLoader.loadClass("example.AuthorFactory").getMethod("create").invoke(null)
        assertEquals("Hello Ada Lovelace", entityClass.getMethod("getSimple").invoke(author))
        assertEquals(listOf("Lovelace", "Ada"), entityClass.getMethod("getComplex").invoke(author))
        assertEquals("ADA LOVELACE", entityClass.getMethod("getInheritedSimple").invoke(author))
        assertEquals(mapOf("Ada" to 3, "Lovelace" to 8), entityClass.getMethod("getInheritedComplex").invoke(author))
    }

    @Test
    fun `implemented getters still reject persistence annotations`() {
        for (annotation in listOf("@Column", "@get:Column", "@Transient", "@get:Transient")) {
            val result = compileGetter(annotation)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
            assertContains(result.messages, "any jimmer annotations except @org.babyfish.jimmer.Formula")
        }
    }

    @Test
    fun `implemented formulas still require dependencies at either annotation target`() {
        for (annotation in listOf("@Formula", "@get:Formula")) {
            val result = compileGetter(annotation)
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
            assertContains(result.messages, "the `dependencies` of that annotation must be specified")
        }
    }

    private fun compileGetter(annotation: String): JvmCompilationResult =
        createCompilation().apply {
            sources = listOf(SourceFile.kotlin("Author.kt", """
                package example
                import org.babyfish.jimmer.Formula
                import org.babyfish.jimmer.sql.*
                @Entity
                interface Author {
                    @Id val id: Long
                    val name: String
                    $annotation
                    val computed: String get() = name.uppercase()
                }
            """.trimIndent()))
        }.compile()
}
