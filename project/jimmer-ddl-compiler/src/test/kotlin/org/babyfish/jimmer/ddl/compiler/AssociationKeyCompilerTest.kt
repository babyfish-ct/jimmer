package org.babyfish.jimmer.ddl.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringWriter
import java.sql.DriverManager
import java.sql.SQLException
import javax.tools.ToolProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.babyfish.jimmer.ddl.compiler.apt.JimmerDdlCompilerAptProcessor
import org.babyfish.jimmer.ddl.compiler.ksp.JimmerDdlCompilerProcessorProvider
import org.babyfish.jimmer.ksp.JimmerProcessorProvider
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import site.addzero.ddlgenerator.core.diff.SchemaDiffPlanner
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.dialect.h2.H2AutoDdlDialect

@OptIn(ExperimentalCompilerApi::class)
class AssociationKeyCompilerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `association keys survive KSP snapshots and enforce database constraints`() {
        val output = temporaryFolder.root.resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        compile(output, "create")
        val sql = output.resolve("create.sql").readText()
        assertTrue(sql.contains("owner_account_id"), sql)
        assertFalse(sql.contains("ownerAccount"), sql)
        val settings = JimmerDdlCompilerSettings(outputDir = output.absolutePath)
        acceptGeneratedSnapshot(settings)
        val tables = JimmerDdlEntityTableSnapshot.readSnapshot(settings).tableSchemas
        assertEquals(listOf("owner_account_id", "code"), tables.getValue("membership").indexes.single().columnNames)
        assertEquals(2, tables.getValue("grouped_membership").indexes.size)
        assertEquals(2, tables.getValue("keyed_profile").indexes.size)
        assertEquals(listOf("tenant_fk", "number_fk", "code"), tables.getValue("composite_member").indexes.single().columnNames)
        val containerIndexes = tables.getValue("container_membership").indexes
        assertEquals(setOf(listOf("account_fk", "code"), listOf("account_fk", "alias")),
            containerIndexes.map { it.columnNames }.toSet())
        assertEquals(listOf("account_pk"), tables.getValue("account").columns.map { it.name })
        assertTrue(tables.getValue("optional_membership").indexes.isEmpty())
        assertTrue(tables.getValue("fake_membership").foreignKeys.isEmpty())

        val h2Project = temporaryFolder.root.resolve("h2")
        val h2Output = h2Project.resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        compile(h2Output, "create", databaseType = "h2", projectDir = h2Project)
        val h2Settings = JimmerDdlCompilerSettings(outputDir = h2Output.absolutePath)
        acceptGeneratedSnapshot(h2Settings)
        val h2Tables = JimmerDdlEntityTableSnapshot.readSnapshot(h2Settings).tableSchemas
        DriverManager.getConnection("jdbc:h2:mem:association_keys;DATABASE_TO_LOWER=TRUE").use { connection ->
            connection.createStatement().use { statement ->
                val dialect = H2AutoDdlDialect()
                val schema = dialect.normalizeSchema(AutoDdlSchema(h2Tables.values.toList()))
                val statements = dialect.render(SchemaDiffPlanner.plan(schema, AutoDdlSchema(emptyList())))
                statements.forEach { statement.execute(it) }
                statement.execute("insert into account(account_pk) values (1), (2)")
                statement.execute("insert into membership(id, owner_account_id, code) values (1, 1, 'A'), (2, 2, 'A'), (3, 1, 'B')")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into membership(id, owner_account_id, code) values (4, 1, 'A')")
                }.sqlState)
                assertEquals("23506", assertFailsWith<SQLException> {
                    statement.execute("insert into membership(id, owner_account_id, code) values (5, 99, 'C')")
                }.sqlState)
                statement.execute("insert into ordinary_membership(id, owner_account_id) values (1, 1), (2, 1)")
                statement.execute("insert into optional_membership(id, owner_account_id, code) values (1, null, 'A'), (2, 1, 'A')")
                statement.execute("insert into profile(id, account_fk) values (1, 1), (2, null), (3, null)")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into profile(id, account_fk) values (4, 1)")
                }.sqlState)
                statement.execute("insert into keyed_profile(id, account_fk, code) values (1, 1, 'A')")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into keyed_profile(id, account_fk, code) values (2, 1, 'B')")
                }.sqlState)
                statement.execute("insert into grouped_membership(id, account_fk, code, alias) values (1, 1, 'A', 'X'), (2, 2, 'A', 'X')")
                for (values in listOf("(3, 1, 'A', 'Y')", "(4, 1, 'B', 'X')")) {
                    assertEquals("23505", assertFailsWith<SQLException> {
                        statement.execute("insert into grouped_membership(id, account_fk, code, alias) values $values")
                    }.sqlState)
                }
                statement.execute("insert into fake_membership(id, account_fk) values (1, 99)")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into fake_membership(id, account_fk) values (2, 99)")
                }.sqlState)
                statement.execute("insert into composite_account(tenant_id, account_number) values (1, 10), (2, 10)")
                statement.execute("insert into composite_member(id, tenant_fk, number_fk, code) values (1, 1, 10, 'A'), (2, 2, 10, 'A')")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into composite_member(id, tenant_fk, number_fk, code) values (3, 1, 10, 'A')")
                }.sqlState)
                assertEquals("23506", assertFailsWith<SQLException> {
                    statement.execute("insert into composite_member(id, tenant_fk, number_fk, code) values (4, 1, 99, 'B')")
                }.sqlState)
            }
        }
        compile(output, "repeat")
        assertFalse(output.resolve("repeat.sql").exists())
        assertEquals(tables, JimmerDdlEntityTableSnapshot.readSnapshot(settings).tableSchemas)
    }

    @Test
    fun `APT expands repeated key containers onto the same physical foreign key columns`() {
        val output = temporaryFolder.root.resolve("apt/build/generated/jimmer-ddl/main/resources/db/migration")
        val declarations = mapOf(
            "Account" to """
                @Entity @Table(name = "account") public interface Account {
                    @Id @Column(name = "account_pk") long id();
                }
            """,
            "GroupedMembership" to """
                @Entity @Table(name = "grouped_membership") public interface GroupedMembership {
                    @Id long id();
                    @Key(group = "code") @Key(group = "alias")
                    @ManyToOne @JoinColumn(name = "account_fk") Account ownerAccount();
                    @Key(group = "code") String code();
                    @Key(group = "alias") String alias();
                    @org.jetbrains.annotations.Nullable String nullableCode();
                    Long optionalNumber();
                    @Key(group = "optional") @ManyToOne @org.jetbrains.annotations.Nullable Account optionalAccount();
                    @Key(group = "optional") String optionalKey();
                }
            """,
            "Profile" to """
                @Entity @Table(name = "profile") public interface Profile {
                    @Id long id();
                    @OneToOne @JoinColumn(name = "account_fk") Account account();
                }
            """,
        )
        val sourceDir = temporaryFolder.root.resolve("apt/src/main/java").apply { mkdirs() }
        val files = declarations.map { (name, declaration) ->
            sourceDir.resolve("$name.java").apply {
                writeText("package demo;\nimport org.babyfish.jimmer.sql.*;\n" + declaration.trimIndent())
            }
        }
        val messages = StringWriter()
        val compiler = ToolProvider.getSystemJavaCompiler()
        val classpath = listOf(org.babyfish.jimmer.sql.Entity::class.java, Unit::class.java, org.jetbrains.annotations.Nullable::class.java)
            .map { File(it.protectionDomain.codeSource.location.toURI()).absolutePath }
            .joinToString(File.pathSeparator)
        compiler.getStandardFileManager(null, null, null).use { manager ->
            val options = listOf(
                "-proc:only", "-classpath", classpath,
                "-AjimmerDdl.outputDir=${output.absolutePath}",
                "-AjimmerDdl.databaseType=postgresql",
                "-AjimmerDdl.outputFormat=plain",
                "-AjimmerDdl.description=create",
                "-AjimmerDdl.compareDatabase=false",
            )
            val task = compiler.getTask(messages, manager, null, options, null, manager.getJavaFileObjectsFromFiles(files))
            task.setProcessors(listOf(JimmerDdlCompilerAptProcessor()))
            assertTrue(task.call(), messages.toString())
        }
        val sql = output.resolve("create.sql").readText()
        assertTrue(sql.contains("\"account_fk\", \"code\""), sql)
        assertTrue(sql.contains("\"account_fk\", \"alias\""), sql)
        assertTrue(sql.contains("uk_profile_account_fk"), sql)
        assertFalse(sql.contains("uk_grouped_membership_optional"), sql)
        assertTrue(sql.contains("\"nullable_code\" TEXT"), sql)
        assertFalse(sql.contains("\"nullable_code\" TEXT NOT NULL"), sql)
        assertFalse(sql.contains("\"optional_number\" BIGINT NOT NULL"), sql)
        assertTrue(sql.contains("REFERENCES \"account\" (\"account_pk\")"), sql)
    }

    private fun acceptGeneratedSnapshot(settings: JimmerDdlCompilerSettings) {
        val generated = JimmerDdlCompilerFiles.resolveGeneratedSnapshotDirectory(settings)
        val accepted = requireNotNull(JimmerDdlCompilerFiles.resolveSnapshotDirectory(settings))
        accepted.parentFile.mkdirs()
        generated.copyRecursively(accepted, overwrite = true)
    }

    private fun compile(
        output: File,
        description: String,
        databaseType: String = "postgresql",
        projectDir: File = temporaryFolder.root,
    ) {
        val declarations = listOf(
            """
            @Entity @Table(name = "account") interface Account {
                @Id @Column(name = "account_pk") val id: Long
                @OneToOne(mappedBy = "account") val profile: Profile?
            }
            """,
            """
            @Entity @Table(name = "membership") interface Membership {
                @Id val id: Long
                @Key @ManyToOne val ownerAccount: Account
                @Key val code: String
            }
            """,
            """
            @Entity @Table(name = "grouped_membership") interface GroupedMembership {
                @Id val id: Long
                @Key(group = "code") @Key(group = "alias")
                @ManyToOne @JoinColumn(name = "account_fk") val ownerAccount: Account
                @Key(group = "code") val code: String
                @Key(group = "alias") val alias: String
            }
            """,
            """
            @Entity @Table(name = "optional_membership") interface OptionalMembership {
                @Id val id: Long
                @Key @ManyToOne val ownerAccount: Account?
                @Key val code: String
            }
            """,
            """
            @Entity @Table(name = "container_membership") interface ContainerMembership {
                @Id val id: Long
                @Keys(value = [Key(group = "code"), Key(group = "alias")])
                @ManyToOne @JoinColumns(value = [JoinColumn(name = "account_fk")])
                val ownerAccount: Account
                @Key(group = "code") val code: String
                @Key(group = "alias") val alias: String
            }
            """,
            """
            @Entity @Table(name = "ordinary_membership") interface OrdinaryMembership {
                @Id val id: Long
                @ManyToOne val ownerAccount: Account
            }
            """,
            """
            @Entity @Table(name = "fake_membership") interface FakeMembership {
                @Id val id: Long
                @Key @ManyToOne(inputNotNull = true)
                @JoinColumn(name = "account_fk", foreignKeyType = ForeignKeyType.FAKE) val ownerAccount: Account?
            }
            """,
            """
            @Entity @Table(name = "profile") interface Profile {
                @Id val id: Long
                @OneToOne @JoinColumn(name = "account_fk") val account: Account?
            }
            """,
            """
            @Entity @Table(name = "keyed_profile") interface KeyedProfile {
                @Id val id: Long
                @Key(group = "account_fk") @OneToOne @JoinColumn(name = "account_fk") val account: Account
                @Key(group = "account_fk") val code: String
            }
            """,
            """
            @Embeddable interface AccountId {
                @Column(name = "tenant_id") val tenant: Long
                @Column(name = "account_number") val number: Int
            }
            """,
            """
            @Entity @Table(name = "composite_account") interface CompositeAccount {
                @Id val id: AccountId
            }
            """,
            """
            @Entity @Table(name = "composite_member") interface CompositeMember {
                @Id val id: Long
                @Key @ManyToOne
                @JoinColumn(name = "number_fk", referencedColumnName = "account_number")
                @JoinColumn(name = "tenant_fk", referencedColumnName = "tenant_id", foreignKeyType = ForeignKeyType.REAL)
                val account: CompositeAccount
                @Key val code: String
            }
            """,
        )

        val messages = ByteArrayOutputStream()
        val result = KotlinCompilation().apply {
            workingDir = projectDir.resolve("src/compilation/$description")
            sources = declarations.mapIndexed { index, declaration ->
                SourceFile.kotlin("Entity$index.kt", "package demo\nimport org.babyfish.jimmer.sql.*\n" + declaration.trimIndent())
            }
            inheritClassPath = true
            jvmTarget = requireNotNull(System.getProperty("jimmer.test.jvmTarget")) {
                "Missing jimmer.test.jvmTarget; run the DDL tests through Gradle"
            }
            languageVersion = "2.1"
            apiVersion = "2.1"
            kotlincArguments = listOf("-Xskip-metadata-version-check")
            verbose = false
            messageOutputStream = messages
            useKsp2()
            symbolProcessorProviders = mutableListOf(JimmerProcessorProvider(), JimmerDdlCompilerProcessorProvider())
            kspProcessorOptions = mutableMapOf(
                "jimmerDdl.enabled" to "true",
                "jimmerDdl.databaseType" to databaseType,
                "jimmerDdl.outputFormat" to "plain",
                "jimmerDdl.outputDir" to output.absolutePath,
                "jimmerDdl.description" to description,
                "jimmerDdl.compareDatabase" to "false",
            )
        }.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, messages.toString())
    }
}
