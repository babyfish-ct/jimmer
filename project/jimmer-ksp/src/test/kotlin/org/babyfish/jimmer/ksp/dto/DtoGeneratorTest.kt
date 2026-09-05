package org.babyfish.jimmer.ksp.dto

import com.tschuchort.compiletesting.*
import org.babyfish.jimmer.ksp.JimmerProcessorProvider
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.*

class DtoGeneratorTest : AbstractTest() {
    @Test
    @OptIn(ExperimentalCompilerApi::class)
    fun `dto file generates a view class`() {
        val compilation = prepare(
            """
                package org.example
                
                import org.babyfish.jimmer.sql.*
                import java.math.BigDecimal
                import javax.validation.constraints.NotEmpty
                import javax.validation.constraints.Positive
                import javax.validation.constraints.PositiveOrZero
                
                @Entity
                @KeyUniqueConstraint
                interface Book {
                    /**
                     * The id property
                     */
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    val id: Long
                
                    /**
                     * The name property, 100% immutable
                     */
                    @Key
                    val name: @NotEmpty(message = "The book name cannot be empty") String
                
                    /**
                     * The edition property, 100% immutable
                     */
                    @Key
                    val edition: @PositiveOrZero Int
                
                    /**
                     * The price property, 100% immutable
                     */
                    val price: @Positive BigDecimal
                }
            """.trimIndent(),
            """
                export org.example.Book
                
                BookView {
                    id
                    name
                    edition
                    price
                }
            """.trimIndent(),
        )

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val bookViewFile = compilation.kspSourcesDir.resolve("kotlin/org/example/dto/BookView.kt")
        assertTrue(bookViewFile.exists())
        assertContains(bookViewFile.readText(), "public open class BookView")
    }

    @Test
    @OptIn(ExperimentalCompilerApi::class)
    fun `jackson annotation without applicable target is not forced onto getter`() {
        val compilation = prepare(
            """
                package org.example
                
                import org.babyfish.jimmer.sql.*
                import javax.validation.constraints.NotEmpty
                
                @Entity
                @KeyUniqueConstraint
                interface Book {
                    /**
                     * The id property
                     */
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    val id: Long
                
                    /**
                     * The name property, 100% immutable
                     */
                    @Key
                    val name: @NotEmpty(message = "The book name cannot be empty") String
                }
            """.trimIndent(),
            """
                export org.example.Book
                
                import com.fasterxml.jackson.annotation.JsonAutoDetect
                import com.fasterxml.jackson.annotation.JsonIgnore
                
                BookView {
                    @JsonIgnore
                    id
                    
                    @JsonAutoDetect
                    name
                }
            """.trimIndent(),
        )

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val bookViewFile = compilation.kspSourcesDir.resolve("kotlin/org/example/dto/BookView.kt")
        assertTrue(bookViewFile.exists())

        val bookView = bookViewFile.readText()
        assertContains(bookView, "public open class BookView")
        assertContains(bookView, "@get:JsonIgnore")
        assertFalse("JsonAutoDetect" in bookView)
    }

    @Test
    fun `immutable dto properties are not shadowed by parameters or local variables`() {
        checkShadowing(false)
    }

    @Test
    fun `mutable dto properties are not shadowed by parameters or local variables`() {
        checkShadowing(true)
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun checkShadowing(mutable: Boolean) {
        val compilation = prepare(
            """
                package org.example

                import org.babyfish.jimmer.sql.*

                @Entity
                interface Book {
                    @Id
                    val id: Long
                    val name: String
                }
            """.trimIndent(),
            """
                export org.example.Book

                ShadowingView {
                    name?
                    _hash: String?
                    other: String?
                    _other: Array<String>?
                }

                ShadowingConversionView {
                    name as _draft
                }

                dynamic input ShadowingValidationInput {
                    name?
                }

                dynamic input ShadowingDynamicInput {
                    name? as _draft
                }

                fuzzy input ShadowingInput {
                    name? as builder
                    dynamic id?
                    separator: String?
                    _input: String?
                }
            """.trimIndent()
        )
        compilation.kspProcessorOptions = mutableMapOf(
            "jimmer.dto.hibernateValidatorEnhancement" to "true",
            "jimmer.dto.mutable" to mutable.toString()
        )
        compilation.sources += SourceFile.kotlin(
            "Checks.kt",
            """
                package org.example

                import org.example.dto.*
                import kotlin.test.*

                fun checkShadowing() {
                    val view = ShadowingView("book", "hash value", "other value", arrayOf("one", "two"))
                    val validation = ShadowingValidationInput("book")
                    assertEquals("book", validation.`${'$'}${'$'}_hibernateValidator_getFieldValue`("name"))
                    assertEquals("book", validation.`${'$'}${'$'}_hibernateValidator_getGetterValue`("getName"))
                    val empty = validation.copy(name = null)
                    assertNull(empty.`${'$'}${'$'}_hibernateValidator_getFieldValue`("name"))
                    assertNull(empty.`${'$'}${'$'}_hibernateValidator_getGetterValue`("getName"))
                    assertFailsWith<IllegalArgumentException> {
                        validation.`${'$'}${'$'}_hibernateValidator_getFieldValue`("missing")
                    }
                    assertFailsWith<IllegalArgumentException> {
                        validation.`${'$'}${'$'}_hibernateValidator_getGetterValue`("missing")
                    }
                    assertEquals(view, view.copy(_other = arrayOf("one", "two")))
                    assertNotEquals(view, view.copy(other = "different"))
                    assertNotEquals(view, view.copy(_other = arrayOf("different")))
                    var hash = "book".hashCode()
                    hash = hash * 31 + "hash value".hashCode()
                    hash = hash * 31 + "other value".hashCode()
                    hash = hash * 31 + arrayOf("one", "two").contentHashCode()
                    assertEquals(hash, view.hashCode())

                    assertEquals("book", ShadowingConversionView("book").toEntity().name)
                    val dynamic = ShadowingDynamicInput("book")
                    assertEquals("book", dynamic.toEntity().name)
                    assertEquals(dynamic, dynamic.copy())
                    assertNotEquals(dynamic, dynamic.copy(is_draftLoaded = false))
                    ${if (mutable) "dynamic._draft = \"changed\"; assertEquals(\"changed\", dynamic.toEntity().name)" else ""}

                    assertEquals("ShadowingInput(separator=null, _input=null)", ShadowingInput.Builder().build().toString())
                    val input = ShadowingInput.Builder()
                        .builder("book")
                        .separator("separator value")
                        ._input("input value")
                        .build()
                    assertEquals("book", input.builder)
                    assertEquals("separator value", input.separator)
                    assertEquals("input value", input._input)
                    assertEquals("ShadowingInput(builder=book, separator=separator value, _input=input value)", input.toString())
                }
            """.trimIndent()
        )
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        result.classLoader.loadClass("org.example.ChecksKt").getMethod("checkShadowing").invoke(null)
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun prepare(entity: String, dto: String): KotlinCompilation {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            symbolProcessorProviders = mutableListOf(JimmerProcessorProvider())
            inheritClassPath = true
            sources = listOf(SourceFile.kotlin("Entity.kt", entity))
        }
        compilation.workingDir.resolve("src/main/dto").mkdirs()
        compilation.workingDir.resolve("src/main/dto/Entity.dto").writeText(dto)
        return compilation
    }
}
