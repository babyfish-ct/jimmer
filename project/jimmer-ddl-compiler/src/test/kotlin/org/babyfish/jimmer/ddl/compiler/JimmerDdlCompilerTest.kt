package org.babyfish.jimmer.ddl.compiler

import java.io.File
import java.nio.charset.StandardCharsets
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.babyfish.jimmer.ddl.compiler.apt.JimmerDdlCompilerAptProcessor
import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.lsi.anno.LsiAnnotation
import site.addzero.lsi.clazz.LsiClass
import site.addzero.lsi.field.LsiField
import site.addzero.lsi.method.LsiMethod
import site.addzero.lsi.type.LsiType
import site.addzero.util.db.DatabaseType
import kotlin.io.path.createTempDirectory

class JimmerDdlCompilerTest {

    @Test
    fun `apt processor generates ddl file from java jimmer entity`() {
        val projectDir = createTempDirectory(prefix = "jimmer-ddl-apt-test")
            .toFile()
        val sourceDir = projectDir.resolve("src/main/java")
        val classesDir = projectDir.resolve("build/classes")
        val outputDir = projectDir.resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        writeJavaSource(
            sourceDir = sourceDir,
            path = "org/babyfish/jimmer/sql/Entity.java",
            content = """
                package org.babyfish.jimmer.sql;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Entity {}
            """.trimIndent(),
        )
        writeJavaSource(
            sourceDir = sourceDir,
            path = "org/babyfish/jimmer/sql/Table.java",
            content = """
                package org.babyfish.jimmer.sql;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Table {
                    String name() default "";
                }
            """.trimIndent(),
        )
        writeJavaSource(
            sourceDir = sourceDir,
            path = "org/babyfish/jimmer/sql/Id.java",
            content = """
                package org.babyfish.jimmer.sql;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Target(ElementType.METHOD)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Id {}
            """.trimIndent(),
        )
        writeJavaSource(
            sourceDir = sourceDir,
            path = "org/babyfish/jimmer/sql/Column.java",
            content = """
                package org.babyfish.jimmer.sql;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Target(ElementType.METHOD)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Column {
                    String name() default "";
                }
            """.trimIndent(),
        )
        writeJavaSource(
            sourceDir = sourceDir,
            path = "demo/AptBook.java",
            content = """
                package demo;

                import org.babyfish.jimmer.sql.Column;
                import org.babyfish.jimmer.sql.Entity;
                import org.babyfish.jimmer.sql.Id;
                import org.babyfish.jimmer.sql.Table;

                @Entity
                @Table(name = "apt_book")
                public interface AptBook {
                    @Id
                    long id();

                    @Column(name = "title")
                    String title();

                    @Column(name = "subtitle")
                    String subtitle();
                }
            """.trimIndent(),
        )

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: error("JDK compiler is required to run APT integration tests")
        val sourceFiles = sourceDir.walkTopDown()
            .filter { file -> file.isFile && file.extension == "java" }
            .toList()
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
                    "-AjimmerDdl.databaseType=postgresql",
                    "-AjimmerDdl.outputFormat=plain",
                    "-AjimmerDdl.outputDir=${outputDir.absolutePath}",
                    "-AjimmerDdl.description=apt_generated",
                    "-AjimmerDdl.compareDatabase=false",
                ),
                null,
                fileManager.getJavaFileObjectsFromFiles(sourceFiles),
            )
            task.setProcessors(listOf(JimmerDdlCompilerAptProcessor()))
            val compiled = task.call()
            assertTrue(compiled, diagnostics.toErrorMessage())
        }

        val sqlFile = outputDir.resolve("apt_generated.sql")
        assertTrue(sqlFile.isFile, "APT should generate ddl file: ${sqlFile.absolutePath}")
        val sql = sqlFile.readText()
        assertContains(sql, """CREATE TABLE IF NOT EXISTS "apt_book"""")
        assertContains(sql, """"id" BIGINT NOT NULL""")
        assertContains(sql, """"title" TEXT""")
        assertContains(sql, """"subtitle" TEXT""")
        assertContains(sql, """ALTER TABLE "apt_book" ALTER COLUMN "title" DROP NOT NULL;""")
        assertContains(sql, """ALTER TABLE "apt_book" ALTER COLUMN "subtitle" DROP NOT NULL;""")

        val snapshotDirectory = projectDir.resolve("build/generated/jimmer-ddl/main/resources/.jimmer-ddl/entity-table-snapshot")
        val snapshotFile = snapshotDirectory.listFiles { file -> file.extension == "properties" }
            ?.singleOrNull()
        assertTrue(snapshotFile?.isFile == true, "APT should generate one table snapshot under: ${snapshotDirectory.absolutePath}")
        assertContains(snapshotFile.readText(), "entity.demo.AptBook=apt_book")
    }

    @Test
    fun `table annotation rename emits rename table operation from entity snapshot`() {
        val outputDir = createTempDirectory(prefix = "jimmer-ddl-test")
            .toFile()
            .resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = outputDir.absolutePath,
            compareDatabase = false,
        )
        val originalEntity = bookEntity(tableName = "biz_user")
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = listOf(originalEntity),
            schema = AutoDdlSchema(
                listOf(
                    AutoDdlTable(
                        "biz_user",
                        listOf(
                            AutoDdlColumn("id", AutoDdlLogicalType.INT64, false, null, null, null, null, null, true, false, null, null),
                        ),
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                    )
                ),
                emptyList(),
            ),
            settings = settings,
        )

        val renamedEntity = bookEntity(tableName = "biz_user_ext")
        val renamedSchema = AutoDdlSchema(
            listOf(
                AutoDdlTable(
                    "biz_user_ext",
                    listOf(
                        AutoDdlColumn("id", AutoDdlLogicalType.INT64, false, null, null, null, null, null, true, false, null, null),
                    ),
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                )
            ),
            emptyList(),
        )

        val operations = JimmerDdlEntityTableSnapshot.planRenameTables(
            entities = listOf(renamedEntity),
            schema = renamedSchema,
            settings = settings,
        )

        assertEquals(listOf(RenameTable("biz_user", "biz_user_ext")), operations)
    }

    @Test
    fun `snapshot stores each table in an independent structural lockfile`() {
        val projectDir = createTempDirectory(prefix = "jimmer-ddl-test").toFile()
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = projectDir.resolve("build/generated/jimmer-ddl/main/resources/db/migration").absolutePath,
            compareDatabase = false,
            sourceFingerprint = "first-source-fingerprint",
        )
        val entities = listOf(
            bookEntity(),
            bookEntity(
                simpleName = "Author",
                qualifiedName = "demo.Author",
                tableName = "author",
            ),
        )
        val result = JimmerDdlCompiler.compile(
            classes = entities,
            settings = settings,
        )

        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = result.entities,
            schema = result.snapshotSchema,
            settings = settings,
        )

        val snapshotDirectory = projectDir.resolve(".jimmer-ddl/entity-table-snapshot")
        val initialFiles = snapshotDirectory.listFiles { file -> file.extension == "properties" }
            ?.sortedBy { file -> file.name }
            .orEmpty()
        assertEquals(2, initialFiles.size)
        assertTrue(initialFiles.all { file -> "__sourceFingerprint" !in file.readText() })
        val initialContents = initialFiles.associate { file -> file.name to file.readBytes() }

        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = result.entities,
            schema = result.snapshotSchema,
            settings = settings.copy(sourceFingerprint = "second-source-fingerprint"),
        )

        val rewrittenFiles = snapshotDirectory.listFiles { file -> file.extension == "properties" }
            ?.associate { file -> file.name to file.readBytes() }
            .orEmpty()
        assertEquals(initialContents.keys, rewrittenFiles.keys)
        initialContents.forEach { (name, content) ->
            assertTrue(content.contentEquals(rewrittenFiles.getValue(name)))
        }

        val changed = JimmerDdlCompiler.compile(
            classes = listOf(
                bookEntity(extraField = true),
                entities.last(),
            ),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = changed.entities,
            schema = changed.snapshotSchema,
            settings = settings,
        )

        val finalContents = snapshotDirectory.listFiles { file -> file.extension == "properties" }
            .orEmpty()
            .associate { file -> file.name to file.readBytes() }
        val authorFileName = initialFiles.single { file -> "tableName=author" in file.readText() }.name
        val bookFileName = initialFiles.single { file -> "tableName=book" in file.readText() }.name
        assertTrue(initialContents.getValue(authorFileName).contentEquals(finalContents.getValue(authorFileName)))
        assertFalse(initialContents.getValue(bookFileName).contentEquals(finalContents.getValue(bookFileName)))
    }

    @Test
    fun `rewriting snapshot removes lockfiles for deleted tables`() {
        val projectDir = createTempDirectory(prefix = "jimmer-ddl-test").toFile()
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = projectDir.resolve("build/generated/jimmer-ddl/main/resources/db/migration").absolutePath,
            compareDatabase = false,
        )
        val book = bookEntity()
        val author = bookEntity(
            simpleName = "Author",
            qualifiedName = "demo.Author",
            tableName = "author",
        )
        val initial = JimmerDdlCompiler.compile(
            classes = listOf(book, author),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = initial.entities,
            schema = initial.snapshotSchema,
            settings = settings,
        )

        val remaining = JimmerDdlCompiler.compile(
            classes = listOf(book),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = remaining.entities,
            schema = remaining.snapshotSchema,
            settings = settings,
        )

        val snapshot = JimmerDdlEntityTableSnapshot.readSnapshot(settings)
        assertEquals(setOf("book"), snapshot.tableHashes.keys)
        assertEquals(mapOf("demo.Book" to "book"), snapshot.entityTables)
        val snapshotDirectory = projectDir.resolve(".jimmer-ddl/entity-table-snapshot")
        assertEquals(1, snapshotDirectory.listFiles { file -> file.extension == "properties" }.orEmpty().size)
    }

    @Test
    fun `postgresql ddl contains idempotent table column and nullable repair statements`() {
        val entity = bookEntity()
        val result = JimmerDdlCompiler.compile(
            classes = listOf(entity),
            settings = JimmerDdlCompilerSettings(
                databaseType = DatabaseType.POSTGRESQL,
                outputFormat = JimmerDdlOutputFormat.PLAIN,
            )
        )

        assertContains(result.sql, "CREATE TABLE IF NOT EXISTS \"book\"")
        assertContains(result.sql, """"title" TEXT NOT NULL""")
        assertContains(result.sql, """ALTER TABLE "book" ALTER COLUMN "subtitle" DROP NOT NULL;""")
    }

    @Test
    fun `nested jimmer embeddable properties are expanded into leaf columns`() {
        val coordinates = TestClass(
            simpleName = "Coordinates",
            qualifiedName = "demo.Coordinates",
            annotations = listOf(embeddable()),
            fields = listOf(
                TestField(
                    name = "latitude",
                    type = TestType("Long"),
                    typeName = "Long",
                )
            ),
        )
        val address = TestClass(
            simpleName = "Address",
            qualifiedName = "demo.Address",
            annotations = listOf(embeddable()),
            fields = listOf(
                TestField(
                    name = "street",
                    type = TestType("String"),
                    typeName = "String",
                ),
                TestField(
                    name = "coordinates",
                    type = TestType(
                        simpleName = "Coordinates",
                        qualifiedName = "demo.Coordinates",
                        lsiClass = coordinates,
                    ),
                    typeName = "demo.Coordinates",
                    fieldTypeClass = coordinates,
                ),
            ),
        )
        val warehouse = TestClass(
            simpleName = "Warehouse",
            qualifiedName = "demo.Warehouse",
            annotations = listOf(entity(), table("warehouse")),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id()),
                ),
                TestField(
                    name = "address",
                    type = TestType(
                        simpleName = "Address",
                        qualifiedName = "demo.Address",
                        lsiClass = address,
                    ),
                    typeName = "demo.Address",
                    fieldTypeClass = address,
                ),
            ),
        )

        val result = JimmerDdlCompiler.compile(
            classes = listOf(warehouse),
            settings = JimmerDdlCompilerSettings(
                databaseType = DatabaseType.POSTGRESQL,
                outputFormat = JimmerDdlOutputFormat.PLAIN,
                compareDatabase = false,
            ),
        )

        assertContains(result.sql, """"street" TEXT NOT NULL""")
        assertContains(result.sql, """"latitude" BIGINT NOT NULL""")
        assertFalse("\"address\"" in result.sql)
        assertFalse("\"coordinates\"" in result.sql)
    }

    @Test
    fun `same schema snapshot does not emit ddl`() {
        val outputDir = createTempDirectory(prefix = "jimmer-ddl-test")
            .toFile()
            .resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = outputDir.absolutePath,
            compareDatabase = false,
        )
        val entity = bookEntity()
        val first = JimmerDdlCompiler.compile(
            classes = listOf(entity),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = first.entities,
            schema = first.schema,
            settings = settings,
        )

        val second = JimmerDdlCompiler.compile(
            classes = listOf(entity),
            settings = settings,
        )

        assertEquals("", second.sql)
    }

    @Test
    fun `changed schema snapshot only emits incremental ddl`() {
        val outputDir = createTempDirectory(prefix = "jimmer-ddl-test")
            .toFile()
            .resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = outputDir.absolutePath,
            compareDatabase = false,
        )
        val entity = bookEntity()
        val first = JimmerDdlCompiler.compile(
            classes = listOf(entity),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = first.entities,
            schema = first.schema,
            settings = settings,
        )

        val changed = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity(extraField = true)),
            settings = settings,
        )

        assertContains(changed.sql, """ALTER TABLE "book" ADD COLUMN IF NOT EXISTS "summary" TEXT;""")
        assertFalse("CREATE TABLE" in changed.sql)
    }

    @Test
    fun `removed property emits drop column ddl`() {
        val outputDir = createTempDirectory(prefix = "jimmer-ddl-test")
            .toFile()
            .resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = outputDir.absolutePath,
            compareDatabase = false,
        )
        val first = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity(extraField = true)),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = first.entities,
            schema = first.schema,
            settings = settings,
        )

        val changed = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity(extraField = false)),
            settings = settings,
        )

        assertContains(changed.sql, """ALTER TABLE "book" DROP COLUMN IF EXISTS "summary";""")
    }

    @Test
    fun `offline incremental ddl emits structural column changes`() {
        val outputDir = createTempDirectory(prefix = "jimmer-ddl-test")
            .toFile()
            .resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = outputDir.absolutePath,
            compareDatabase = false,
            nullabilityRepairOnly = true,
        )
        val first = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity()),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = first.entities,
            schema = first.schema,
            settings = settings,
        )

        val changed = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity(extraField = true, titleTypeName = "Int")),
            settings = settings,
        )

        assertContains(changed.sql, """ALTER TABLE "book" ADD COLUMN IF NOT EXISTS "summary" TEXT;""")
        assertContains(changed.sql, """ALTER COLUMN "title" TYPE INTEGER""")
        assertContains(changed.sql, """USING CASE""")
        assertTrue(changed.warnings.none { warning -> "skipped column structure changes" in warning })
    }

    @Test
    fun `generated snapshot is staged under generated resources instead of source snapshot`() {
        val projectDir = createTempDirectory(prefix = "jimmer-ddl-test")
            .toFile()
        val outputDir = projectDir.resolve("build/generated/jimmer-ddl/main/resources/db/migration")
        val settings = JimmerDdlCompilerSettings(
            databaseType = DatabaseType.POSTGRESQL,
            outputFormat = JimmerDdlOutputFormat.PLAIN,
            outputDir = outputDir.absolutePath,
            compareDatabase = false,
            sourceFingerprint = "generated-source-fingerprint",
        )
        val first = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity()),
            settings = settings,
        )
        JimmerDdlEntityTableSnapshot.writeSnapshot(
            entities = first.entities,
            schema = first.schema,
            settings = settings,
        )
        val sourceSnapshot = JimmerDdlCompilerFiles.resolveSnapshotDirectory(settings)
        requireNotNull(sourceSnapshot)
        val sourceContent = sourceSnapshot.readSnapshotContents()
        val changed = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity(extraField = true)),
            settings = settings,
        )

        JimmerDdlEntityTableSnapshot.writeGeneratedSnapshot(
            entities = changed.entities,
            schema = changed.schema,
            settings = settings,
        )

        val generatedSnapshot = JimmerDdlCompilerFiles.resolveGeneratedSnapshotDirectory(settings)
        assertTrue(generatedSnapshot.isDirectory)
        assertEquals(sourceContent, sourceSnapshot.readSnapshotContents())
        assertFalse(sourceContent == generatedSnapshot.readSnapshotContents())
        val sourceFingerprint = JimmerDdlCompilerFiles.resolveBuildSourceFingerprintFile(settings)
        requireNotNull(sourceFingerprint)
        assertTrue(sourceFingerprint.isFile)
        assertContains(sourceFingerprint.readText(), "__sourceFingerprint=generated-source-fingerprint")
    }

    @Test
    fun `cross module inherited many to many emits junction table without target table`() {
        val user = TestClass(
            simpleName = "User",
            qualifiedName = "site.addzero.crud.model.system.user.User",
            annotations = listOf(entity(), table("system_users")),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id()),
                )
            ),
        )
        val basePersonInCharge = TestClass(
            simpleName = "BasePersonInCharge",
            qualifiedName = "cn.iocoder.yudao.module.ai.power.equipment_information_archive.entity.BasePersonInCharge",
            annotations = listOf(mappedSuperclass()),
            fields = listOf(
                TestField(
                    name = "personInCharge",
                    type = TestType(
                        simpleName = "List",
                        qualifiedName = "kotlin.collections.List",
                        isCollectionType = true,
                        typeParameters = listOf(
                            TestType(
                                simpleName = "User",
                                qualifiedName = "site.addzero.crud.model.system.user.User",
                                lsiClass = user,
                            )
                        ),
                    ),
                    typeName = "List",
                    annotations = listOf(manyToMany()),
                    isCollectionType = true,
                )
            ),
        )
        val device = TestClass(
            simpleName = "EquipmentInformationArchive",
            qualifiedName = "cn.iocoder.yudao.module.ai.power.equipment_information_archive.entity.EquipmentInformationArchive",
            annotations = listOf(entity(), table("ai_power_device")),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id()),
                )
            ),
            interfaces = listOf(basePersonInCharge),
        )

        val result = JimmerDdlCompiler.compile(
            classes = listOf(device.toStableJimmerDdlSnapshot()),
            settings = JimmerDdlCompilerSettings(
                databaseType = DatabaseType.POSTGRESQL,
                outputFormat = JimmerDdlOutputFormat.PLAIN,
                compareDatabase = false,
            ),
        )

        assertContains(result.sql, "CREATE TABLE IF NOT EXISTS \"ai_power_device\"")
        assertContains(result.sql, "CREATE TABLE IF NOT EXISTS \"equipment_information_archive_person_in_charge_mapping\"")
        assertContains(result.sql, "\"equipment_information_archive_id\" BIGINT NOT NULL")
        assertContains(result.sql, "\"user_id\" BIGINT NOT NULL")
        assertFalse("\"system_users\"" in result.sql)
    }

    private fun bookEntity(
        simpleName: String = "Book",
        qualifiedName: String = "demo.Book",
        tableName: String = "book",
        extraField: Boolean = false,
        titleTypeName: String = "String",
    ): TestClass {
        return TestClass(
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            annotations = listOf(entity(), table(tableName)),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id()),
                ),
                TestField(
                    name = "title",
                    type = TestType(titleTypeName),
                    typeName = titleTypeName,
                    annotations = listOf(column("title")),
                ),
                TestField(
                    name = "subtitle",
                    type = TestType("String"),
                    typeName = "String",
                    annotations = listOf(column("subtitle")),
                    isNullable = true,
                ),
            ) + if (extraField) {
                listOf(
                    TestField(
                        name = "summary",
                        type = TestType("String"),
                        typeName = "String",
                        annotations = listOf(column("summary")),
                        isNullable = true,
                    )
                )
            } else {
                emptyList()
            }
        )
    }

    private fun entity(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Entity", "Entity")
    }

    private fun mappedSuperclass(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.MappedSuperclass", "MappedSuperclass")
    }

    private fun embeddable(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Embeddable", "Embeddable")
    }

    private fun table(name: String): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Table", "Table", mapOf("name" to name))
    }

    private fun id(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Id", "Id")
    }

    private fun column(name: String): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Column", "Column", mapOf("name" to name))
    }

    private fun manyToMany(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.ManyToMany", "ManyToMany")
    }

    private fun writeJavaSource(
        sourceDir: File,
        path: String,
        content: String,
    ) {
        val file = sourceDir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun File.readSnapshotContents(): Map<String, String> {
        return listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .associate { file -> file.name to file.readText() }
    }

    private fun DiagnosticCollector<JavaFileObject>.toErrorMessage(): String {
        return diagnostics.joinToString(separator = "\n") { diagnostic ->
            val source = diagnostic.source?.name.orEmpty()
            val position = if (diagnostic.lineNumber > 0) {
                "${diagnostic.lineNumber}:${diagnostic.columnNumber}"
            } else {
                "?:?"
            }
            "${diagnostic.kind} $source:$position ${diagnostic.getMessage(null)}"
        }
    }

    private data class TestAnnotation(
        override val qualifiedName: String?,
        override val simpleName: String?,
        override val attributes: Map<String, Any?> = emptyMap(),
    ) : LsiAnnotation {
        override fun getAttribute(name: String): Any? = attributes[name]

        override fun hasAttribute(name: String): Boolean = attributes.containsKey(name)
    }

    private data class TestType(
        override val simpleName: String?,
        override val qualifiedName: String? = simpleName,
        override val presentableText: String? = simpleName,
        override val annotations: List<LsiAnnotation> = emptyList(),
        override val isCollectionType: Boolean = false,
        override val isNullable: Boolean = false,
        override val typeParameters: List<LsiType> = emptyList(),
        override val isPrimitive: Boolean = false,
        override val componentType: LsiType? = null,
        override val isArray: Boolean = false,
        override val lsiClass: LsiClass? = null,
    ) : LsiType

    private data class TestField(
        override val name: String?,
        override val type: LsiType? = null,
        override val typeName: String? = type?.simpleName,
        override val comment: String? = null,
        override val annotations: List<LsiAnnotation> = emptyList(),
        override val isStatic: Boolean = false,
        override val isConstant: Boolean = false,
        override val isEnum: Boolean = false,
        override val isVar: Boolean = false,
        override val isLateInit: Boolean = false,
        override val isCollectionType: Boolean = false,
        override val defaultValue: String? = null,
        override val columnName: String? = null,
        override val declaringClass: LsiClass? = null,
        override val fieldTypeClass: LsiClass? = null,
        override val isNestedObject: Boolean = false,
        override val children: List<LsiField> = emptyList(),
        override val isNullable: Boolean = false,
    ) : LsiField

    private data class TestClass(
        override val simpleName: String?,
        override val qualifiedName: String? = simpleName,
        override val comment: String? = null,
        override val fields: List<LsiField> = emptyList(),
        override val annotations: List<LsiAnnotation> = emptyList(),
        override val isInterface: Boolean = true,
        override val isEnum: Boolean = false,
        override val isCollectionType: Boolean = false,
        override val isPojo: Boolean = true,
        override val superClasses: List<LsiClass> = emptyList(),
        override val interfaces: List<LsiClass> = emptyList(),
        override val methods: List<LsiMethod> = emptyList(),
        override val fileName: String? = null,
        override val isObject: Boolean = false,
        override val isCompanionObject: Boolean = false,
    ) : LsiClass
}
