package org.babyfish.jimmer.ddl.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import java.io.ByteArrayOutputStream
import java.io.File
import org.babyfish.jimmer.ddl.compiler.ksp.JimmerDdlCompilerProcessorProvider
import org.babyfish.jimmer.ksp.JimmerProcessorProvider
import org.babyfish.jimmer.meta.ImmutableType
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.runtime.ConnectionManager
import java.sql.DriverManager
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
@RunWith(Parameterized::class)
class KotlinComputedPropertyDdlTest(private val kind: String) {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `create table excludes declared and inherited computed getters`() {
        val projectDir = temporaryFolder.root.resolve("create").apply { mkdirs() }
        val sql = compile(projectDir, "create", includeComputedProperties = true)

        assertContains(sql, """CREATE TABLE IF NOT EXISTS "formula_author"""")
        assertContains(sql, """"id" BIGINT NOT NULL""")
        assertContains(sql, """"first_name" TEXT NOT NULL""")
        assertContains(sql, """"last_name" TEXT NOT NULL""")
        assertComputedColumnMapping(sql)
        assertSnapshotColumns(projectDir, expectedColumns())
    }

    @Test
    fun `incremental ddl adds only persistent properties`() {
        val projectDir = temporaryFolder.root.resolve("incremental").apply { mkdirs() }
        val initialSql = compile(projectDir, "initial", includeComputedProperties = false)
        assertContains(initialSql, """CREATE TABLE IF NOT EXISTS "formula_author"""")
        acceptGeneratedSnapshot(projectDir)

        val sql = compile(projectDir, "updated", includeComputedProperties = true, includeNickname = true)

        assertContains(sql, """ALTER TABLE "formula_author" ADD COLUMN IF NOT EXISTS "nickname" TEXT""")
        assertFalse(sql.contains("CREATE TABLE"), sql)
        assertComputedColumnMapping(sql)
        assertSnapshotColumns(projectDir, expectedColumns() + "nickname")
    }

    @Test
    fun `adding only computed properties leaves the schema unchanged`() {
        val projectDir = temporaryFolder.root.resolve("computed-only").apply { mkdirs() }
        compile(projectDir, "initial", includeComputedProperties = false)
        acceptGeneratedSnapshot(projectDir)

        compile(projectDir, "computed", includeComputedProperties = true, expectedMigration = false)
        assertSnapshotColumns(projectDir, expectedColumns())
        compile(projectDir, "repeat", includeComputedProperties = true, expectedMigration = false)
    }

    private fun compile(
        projectDir: File,
        description: String,
        includeComputedProperties: Boolean,
        includeNickname: Boolean = false,
        expectedMigration: Boolean = true,
    ): String {
        val inheritedProperty = if (includeComputedProperties) computedProperty("inheritedComputedValue") else ""
        val declaredProperty = if (includeComputedProperties) computedProperty("computedValue") else ""
        val nickname = if (includeNickname) "val nickname: String?" else ""
        val imports = """
                package demo

                import org.babyfish.jimmer.Formula
                import org.babyfish.jimmer.sql.Entity
                import org.babyfish.jimmer.sql.Id
                import org.babyfish.jimmer.sql.MappedSuperclass
                import org.babyfish.jimmer.sql.Table
                import org.babyfish.jimmer.sql.Transient
                import org.babyfish.jimmer.sql.kt.KTransientResolver
            """.trimIndent()
        val resolverSource = """
                $imports
                class AuthorNameResolver : KTransientResolver<Long, String> {
                    override fun resolve(ids: Collection<Long>): Map<Long, String> =
                        ids.associateWith { "author-" + it }
                }
            """.trimIndent()
        val baseSource = """
                $imports
                @MappedSuperclass
                interface FormulaBase {
                    val firstName: String
                    val lastName: String
                    $inheritedProperty
                }
            """.trimIndent()
        val entitySource = """
                $imports
                @Entity
                @Table(name = "formula_author")
                interface FormulaAuthor : FormulaBase {
                    @Id
                    val id: Long
                    $declaredProperty
                    $nickname
                }
            """.trimIndent()
        val outputDir = outputDir(projectDir)
        val messages = ByteArrayOutputStream()
        val compilation = KotlinCompilation().apply {
            jvmTarget = requireNotNull(System.getProperty("jimmer.test.jvmTarget"))
            // 编译测试工具使用 Kotlin 2.1，不能从传递依赖的新版标准库推断语言版本。
            languageVersion = "2.1"
            apiVersion = "2.1"
            kotlincArguments = listOf("-Xskip-metadata-version-check")
            useKsp2()
            inheritClassPath = true
            verbose = false
            workingDir = projectDir.resolve("src/compilation/$description")
            sources = listOf(
                SourceFile.kotlin("AuthorNameResolver.kt", resolverSource),
                SourceFile.kotlin("FormulaBase.kt", baseSource),
                SourceFile.kotlin("FormulaAuthor.kt", entitySource),
            )
            symbolProcessorProviders = mutableListOf(
                JimmerProcessorProvider(),
                JimmerDdlCompilerProcessorProvider(),
            )
            kspProcessorOptions = mutableMapOf(
                "jimmerDdl.enabled" to "true",
                "jimmerDdl.databaseType" to "postgresql",
                "jimmerDdl.outputFormat" to "plain",
                "jimmerDdl.outputDir" to outputDir.absolutePath,
                "jimmerDdl.description" to description,
                "jimmerDdl.compareDatabase" to "false",
            )
            messageOutputStream = messages
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, messages.toString())
        if (includeComputedProperties) {
            val entityType = ImmutableType.get(result.classLoader.loadClass("demo.FormulaAuthor"))
            for (name in listOf("computedValue", "inheritedComputedValue")) {
                if (kind.startsWith("plain-")) {
                    assertFalse(entityType.props.containsKey(name), name)
                } else {
                    assertFalse(entityType.props.getValue(name).isColumnDefinition, name)
                }
            }
        }
        val sqlFile = outputDir.resolve("$description.sql")
        if (!expectedMigration) {
            assertFalse(sqlFile.exists(), "Computed properties must not generate a migration: $messages")
            return ""
        }
        assertTrue(sqlFile.isFile, "KSP should generate $sqlFile: $messages")
        val sql = sqlFile.readText()
        if (description == "create" && kind.startsWith("plain-")) {
            assertGetterQuery(result, sql)
        }
        return sql
    }

    private fun assertGetterQuery(result: JvmCompilationResult, sql: String) {
        val entityClass = result.classLoader.loadClass("demo.FormulaAuthor")
        DriverManager.getConnection("jdbc:h2:mem:$kind;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE").use { connection ->
            connection.createStatement().use { statement ->
                for (command in sql.split(';').filter { it.isNotBlank() }) {
                    statement.execute(command)
                }
                statement.executeUpdate("insert into formula_author(id, first_name, last_name) values(1, 'Ada', 'Lovelace')")
            }
            val client = JSqlClient.newBuilder()
                .setDialect(H2Dialect())
                .setConnectionManager(ConnectionManager.singleConnectionManager(connection))
                .build()
            val author = client.entities.findById(entityClass, 1L)
            assertEquals("Ada Lovelace", entityClass.getMethod("getComputedValue").invoke(author))
            assertEquals("Ada Lovelace", entityClass.getMethod("getInheritedComputedValue").invoke(author))
        }
    }

    private fun computedProperty(name: String): String {
        val formulaAnnotation = if (kind.startsWith("kotlin")) {
            val target = if (kind.endsWith("getter-target")) "get:" else ""
            """@${target}Formula(dependencies = ["firstName", "lastName"])"""
        } else {
            ""
        }
        return when (kind) {
            "sql" -> """
                @Formula(sql = "%alias.FIRST_NAME || ' ' || %alias.LAST_NAME")
                val $name: String
            """.trimIndent()
            "kotlin", "kotlin-getter-target" -> """
                $formulaAnnotation
                val $name: String
                    get() = firstName + " " + lastName
            """.trimIndent()
            "transient-ref" -> """
                @Transient(ref = "authorNameResolver")
                val $name: String
            """.trimIndent()
            "transient-resolver" -> """
                @Transient(AuthorNameResolver::class)
                val $name: String
            """.trimIndent()
            "plain-getter" -> """
                val $name: String
                    get() = firstName + " " + lastName
            """.trimIndent()
            "kotlin-complex", "kotlin-complex-getter-target", "plain-complex-getter" -> """
                $formulaAnnotation
                val $name: String
                    get() {
                        val parts = listOf(firstName, lastName).filter { it.isNotBlank() }
                        return when {
                            parts.isEmpty() -> "anonymous"
                            parts.size == 1 -> parts.single().uppercase()
                            else -> parts.joinToString(" ")
                        }
                    }
            """.trimIndent()
            "transient" -> """
                @Transient
                val $name: String
            """.trimIndent()
            else -> error("Unknown computed property kind: $kind")
        }
    }

    private fun assertComputedColumnMapping(sql: String) {
        for (column in listOf("computed_value", "inherited_computed_value")) {
            assertFalse(sql.contains("\"$column\""), sql)
        }
    }

    private fun expectedColumns(): Set<String> = setOf("id", "first_name", "last_name")

    private fun assertSnapshotColumns(projectDir: File, expected: Set<String>) {
        acceptGeneratedSnapshot(projectDir)
        val settings = JimmerDdlCompilerSettings(outputDir = outputDir(projectDir).absolutePath)
        val snapshot = JimmerDdlEntityTableSnapshot.readSnapshot(settings)
        val table = snapshot.tableSchemas.getValue("formula_author")
        assertEquals(expected, table.columns.map { it.name }.toSet())
    }

    private fun acceptGeneratedSnapshot(projectDir: File) {
        val settings = JimmerDdlCompilerSettings(outputDir = outputDir(projectDir).absolutePath)
        val generated = JimmerDdlCompilerFiles.resolveGeneratedSnapshotDirectory(settings)
        val accepted = requireNotNull(JimmerDdlCompilerFiles.resolveSnapshotDirectory(settings))
        assertTrue(generated.isDirectory, "KSP should generate a schema snapshot")
        accepted.parentFile.mkdirs()
        accepted.deleteRecursively()
        generated.copyRecursively(accepted, overwrite = true)
    }

    private fun outputDir(projectDir: File): File =
        projectDir.resolve("build/generated/jimmer-ddl/main/resources/db/migration")

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<String>> = listOf(
            arrayOf("sql"),
            arrayOf("kotlin"),
            arrayOf("kotlin-complex"),
            arrayOf("kotlin-getter-target"),
            arrayOf("kotlin-complex-getter-target"),
            arrayOf("transient-ref"),
            arrayOf("transient-resolver"),
            arrayOf("transient"),
            arrayOf("plain-getter"),
            arrayOf("plain-complex-getter"),
        )
    }
}
