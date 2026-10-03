package org.babyfish.jimmer.ksp.dto

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class AnnotationArgumentsTest : AbstractTest() {

    @Test
    fun `existing singleton and nested array syntax compiles`() {
        val result = prepare(
            "@TestAnnotation(numberClass = Int::class, n = N({10, 20}))\nExistingView { name }"
        ).compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `annotation arguments use declared types recursively`() {
        val compilation = prepare(
            """
                @TestAnnotation(numberClass = Int::class, n = N({10, 20}), m = M())
                ArrayView { name }

                @TestAnnotation(n = N(10, 20), enumValue = Choice.B, label = "positional")
                PositionalView { name }

                @TestAnnotation(n = [N(30), N(value = [40, 50])], numberClass = [Int::class, Long::class])
                NamedView { name }

                @TestAnnotation(n = [], numberClass = {})
                EmptyView { name }

                @TestAnnotation
                DefaultView { name }

                @Numbers(10, 20, label = "numbers")
                NumbersView { name }

                @Numbers(10)
                SingletonView { name }

                @LateValue(10, 20)
                LateValueView { name }

                PropertyView {
                    @TestAnnotation(numberClass = Int::class, n = N(60))
                    name
                }
            """.trimIndent()
        )
        compilation.sources += SourceFile.kotlin(
            "Checks.kt",
            """
                package org.example

                import org.example.dto.*
                import kotlin.test.*

                private inline fun <reified A : Annotation> annotation(type: Class<*>): A = type.getAnnotation(A::class.java)

                fun checkAnnotations() {
                    val a = annotation<TestAnnotation>(ArrayView::class.java)
                    assertContentEquals(arrayOf(Int::class), a.numberClass)
                    assertContentEquals(intArrayOf(10, 20), a.n.single().value)
                    assertEquals(7, a.m.value)
                    assertEquals(99, a.num)
                    val p = annotation<TestAnnotation>(PositionalView::class.java)
                    assertContentEquals(intArrayOf(10, 20), p.n.single().value)
                    assertEquals(Choice.B, p.enumValue)
                    assertEquals("positional", p.label)
                    val n = annotation<TestAnnotation>(NamedView::class.java)
                    assertContentEquals(intArrayOf(30), n.n[0].value)
                    assertContentEquals(intArrayOf(40, 50), n.n[1].value)
                    assertContentEquals(arrayOf(Int::class, Long::class), n.numberClass)
                    val e = annotation<TestAnnotation>(EmptyView::class.java)
                    assertTrue(e.n.isEmpty())
                    assertTrue(e.numberClass.isEmpty())
                    val d = annotation<TestAnnotation>(DefaultView::class.java)
                    assertContentEquals(intArrayOf(9), d.n.single().value)
                    assertContentEquals(intArrayOf(10, 20), annotation<Numbers>(NumbersView::class.java).value)
                    assertEquals("numbers", annotation<Numbers>(NumbersView::class.java).label)
                    assertContentEquals(intArrayOf(10), annotation<Numbers>(SingletonView::class.java).value)
                    val late = annotation<LateValue>(LateValueView::class.java)
                    assertEquals("default", late.label)
                    assertContentEquals(intArrayOf(10, 20), late.value)
                    val prop = PropertyView::class.java.getMethod("getName").getAnnotation(TestAnnotation::class.java)
                    assertContentEquals(intArrayOf(60), prop.n.single().value)
                }
            """.trimIndent()
        )
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        result.classLoader.loadClass("org.example.ChecksKt").getMethod("checkAnnotations").invoke(null)
    }

    @Test
    fun `java annotation methods supply expected array types`() {
        val compilation = prepare(
            """
                @JavaAnnotation(numberClass = Int::class, n = JavaAnnotation.N({10, 20}))
                JavaView { name }

                @JavaAnnotation(10, 20, n = JavaAnnotation.N(30, 40))
                JavaPositionalView { name }

                @JavaAnnotation([], numberClass = {}, n = [])
                JavaEmptyView { name }

                @JavaAnnotation(n = JavaAnnotation.N())
                JavaDefaultView { name }
            """.trimIndent()
        )
        compilation.sources += SourceFile.java(
            "JavaAnnotation.java",
            """
                package org.example;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                @Retention(RetentionPolicy.RUNTIME)
                public @interface JavaAnnotation {
                    int[] value() default {7};
                    Class<? extends Number>[] numberClass() default {};
                    N[] n() default {};
                    @Retention(RetentionPolicy.RUNTIME)
                    @interface N { int[] value() default {9}; }
                }
            """.trimIndent()
        )
        compilation.sources += SourceFile.kotlin(
            "JavaChecks.kt",
            """
                package org.example
                import org.example.dto.*
                import kotlin.test.*
                fun checkJavaAnnotations() {
                    fun annotation(type: Class<*>) = type.getAnnotation(JavaAnnotation::class.java)
                    val a = annotation(JavaView::class.java)
                    assertContentEquals(arrayOf(Int::class), a.numberClass)
                    assertContentEquals(intArrayOf(10, 20), a.n.single().value)
                    assertContentEquals(intArrayOf(7), a.value)
                    val p = annotation(JavaPositionalView::class.java)
                    assertContentEquals(intArrayOf(10, 20), p.value)
                    assertContentEquals(intArrayOf(30, 40), p.n.single().value)
                    val e = annotation(JavaEmptyView::class.java)
                    assertTrue(e.value.isEmpty())
                    assertTrue(e.numberClass.isEmpty())
                    assertTrue(e.n.isEmpty())
                    assertContentEquals(intArrayOf(9), annotation(JavaDefaultView::class.java).n.single().value)
                }
            """.trimIndent()
        )
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        result.classLoader.loadClass("org.example.JavaChecksKt").getMethod("checkJavaAnnotations").invoke(null)
    }

    @Test
    fun `library annotations supply expected array types`() {
        val result = prepare(
            """
                @kotlin.Suppress(names = "unused")
                @java.lang.SuppressWarnings("unused", "unchecked")
                LibraryView {
                    @javax.validation.constraints.Pattern.List(javax.validation.constraints.Pattern(regexp = ".*"))
                    name
                }
            """.trimIndent()
        ).compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `positional values cannot fill other parameters or a scalar array`() {
        for ((annotation, message) in listOf(
            "@M(10, 20)" to "not an array",
            "@Pair(10, 20)" to "value",
            "@TestAnnotation(m = M([10, 20]))" to "not an array"
        )) {
            val result = prepare("$annotation\nInvalidView { name }").compile()
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
            assertTrue(message in result.messages, result.messages)
        }
    }

    private fun prepare(dto: String): KotlinCompilation {
        val compilation = createCompilation().apply {
            sources = listOf(SourceFile.kotlin(
                "Entity.kt",
                """
                    package org.example
                    import org.babyfish.jimmer.sql.*
                    import kotlin.reflect.KClass
                    @Entity
                    interface Book {
                        @Id val id: Long
                        val name: String
                    }
                    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY_GETTER)
                    annotation class TestAnnotation(
                        val value: Boolean = false,
                        val numberClass: Array<KClass<out Number>> = [],
                        val num: Int = 99,
                        val m: M = M(),
                        vararg val n: N = [N(9)],
                        val enumValue: Choice = Choice.A,
                        val label: String = "default"
                    )
                    annotation class N(vararg val value: Int)
                    annotation class M(val value: Int = 7)
                    annotation class Numbers(val value: IntArray, val label: String = "default")
                    annotation class LateValue(val label: String = "default", vararg val value: Int)
                    annotation class Pair(val left: Int, val right: Int)
                    enum class Choice { A, B }
                """.trimIndent()
            ))
        }
        compilation.workingDir.resolve("src/main/dto").mkdirs()
        compilation.workingDir.resolve("src/main/dto/Book.dto").writeText(
            "export org.example.Book\n" +
                "import org.example.{TestAnnotation, N, M, Numbers, LateValue, Pair, Choice, JavaAnnotation}\n" + dto
        )
        return compilation
    }
}
