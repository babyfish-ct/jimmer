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