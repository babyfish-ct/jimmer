package org.babyfish.jimmer.ddl.compiler

import java.nio.charset.StandardCharsets
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.babyfish.jimmer.ddl.compiler.apt.JimmerDdlCompilerAptProcessor

class AnnotatedStringDdlTest {

    @Test
    fun `java annotations preserve string length in mysql ddl`() {
        val projectDir = createTempDirectory(prefix = "annotated-string-ddl-test").toFile()
        val sourceFile = projectDir.resolve("src/main/java/demo/CallAlgRecord.java")
        val classesDir = projectDir.resolve("build/classes")
        val outputDir = projectDir.resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(
            """
                package demo;

                import javax.validation.constraints.Pattern;
                import javax.validation.constraints.Size;
                import org.babyfish.jimmer.sql.Column;
                import org.babyfish.jimmer.sql.Entity;
                import org.babyfish.jimmer.sql.Id;
                import org.babyfish.jimmer.sql.Table;

                @Entity
                @Table(name = "call_alg_record")
                public interface CallAlgRecord {
                    @Id
                    @Size(max = 50)
                    @Pattern(regexp = "[^\\d]+\\S+")
                    @Column(sqlType = "varchar")
                    String id();
                }
            """.trimIndent(),
        )

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: error("JDK compiler is required to run APT integration tests")
        classesDir.mkdirs()
        compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8).use { fileManager ->
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(classesDir))
            val task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                listOf(
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-AjimmerDdl.enabled=true",
                    "-AjimmerDdl.databaseType=mysql",
                    "-AjimmerDdl.outputFormat=plain",
                    "-AjimmerDdl.outputDir=${outputDir.absolutePath}",
                    "-AjimmerDdl.description=annotated_string",
                    "-AjimmerDdl.compareDatabase=false",
                ),
                null,
                fileManager.getJavaFileObjects(sourceFile),
            )
            task.setProcessors(listOf(JimmerDdlCompilerAptProcessor()))
            assertTrue(task.call(), diagnostics.toErrorMessage())
        }

        val sqlFile = outputDir.resolve("annotated_string.sql")
        assertTrue(sqlFile.isFile, "APT should generate ddl file: ${sqlFile.absolutePath}")
        assertContains(sqlFile.readText(), "`id` VARCHAR(50)")
    }

    private fun DiagnosticCollector<JavaFileObject>.toErrorMessage(): String =
        diagnostics.joinToString(separator = "\n") { diagnostic ->
            val source = diagnostic.source?.name.orEmpty()
            val position = if (diagnostic.lineNumber > 0) {
                "${diagnostic.lineNumber}:${diagnostic.columnNumber}"
            } else {
                "?:?"
            }
            "${diagnostic.kind} $source:$position ${diagnostic.getMessage(null)}"
        }
}
