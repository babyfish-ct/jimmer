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
        assertContains(sql, """CREATE TABLE "apt_book"""")
        assertContains(sql, """"id" BIGINT NOT NULL""")
        assertContains(sql, """"title" VARCHAR(255)""")
        assertContains(sql, """"subtitle" VARCHAR(255)""")
        assertContains(sql, """ALTER TABLE "apt_book" ALTER COLUMN "title" DROP NOT NULL;""")
        assertContains(sql, """ALTER TABLE "apt_book" ALTER COLUMN "subtitle" DROP NOT NULL;""")

        val snapshotFile = projectDir.resolve("build/generated/jimmer-ddl/main/resources/.jimmer-ddl/entity-table-snapshot.properties")
        assertTrue(snapshotFile.isFile, "APT should generate snapshot file: ${snapshotFile.absolutePath}")
        assertContains(snapshotFile.readText(), "demo.AptBook=apt_book")
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
    fun `postgresql ddl contains idempotent table column and nullable repair statements`() {
        val entity = bookEntity()
        val result = JimmerDdlCompiler.compile(
            classes = listOf(entity),
            settings = JimmerDdlCompilerSettings(
                databaseType = DatabaseType.POSTGRESQL,
                outputFormat = JimmerDdlOutputFormat.PLAIN,
            )
        )

        assertContains(result.sql, "CREATE TABLE \"book\"")
        assertContains(result.sql, """"title" VARCHAR(255) NOT NULL""")
        assertContains(result.sql, """ALTER TABLE "book" ALTER COLUMN "subtitle" DROP NOT NULL;""")
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

        assertContains(changed.sql, """ALTER TABLE "book" ADD COLUMN "summary" VARCHAR(255);""")
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

        assertContains(changed.sql, """ALTER TABLE "book" DROP COLUMN "summary";""")
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

        assertContains(changed.sql, """ALTER TABLE "book" ADD COLUMN "summary" VARCHAR(255);""")
        assertContains(changed.sql, """ALTER TABLE "book" ALTER COLUMN "title" TYPE INTEGER;""")
        assertContains(changed.sql, """ALTER TABLE "book" ALTER COLUMN "title" SET NOT NULL;""")
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
        val sourceSnapshot = JimmerDdlCompilerFiles.resolveSnapshotFile(settings)
        requireNotNull(sourceSnapshot)
        val sourceContent = sourceSnapshot.readText()
        val changed = JimmerDdlCompiler.compile(
            classes = listOf(bookEntity(extraField = true)),
            settings = settings,
        )

        JimmerDdlEntityTableSnapshot.writeGeneratedSnapshot(
            entities = changed.entities,
            schema = changed.schema,
            settings = settings,
        )

        val generatedSnapshot = JimmerDdlCompilerFiles.resolveGeneratedSnapshotFile(settings)
        assertTrue(generatedSnapshot.isFile)
        assertEquals(sourceContent, sourceSnapshot.readText())
        assertFalse(sourceContent == generatedSnapshot.readText())
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

        assertContains(result.sql, "CREATE TABLE \"ai_power_device\"")
        assertContains(result.sql, "CREATE TABLE \"equipment_information_archive_person_in_charge_mapping\"")
        assertContains(result.sql, "\"equipment_information_archive_id\" BIGINT NOT NULL")
        assertContains(result.sql, "\"user_id\" BIGINT NOT NULL")
        assertFalse("CREATE TABLE \"system_users\"" in result.sql)
    }

    private fun bookEntity(
        tableName: String = "book",
        extraField: Boolean = false,
        titleTypeName: String = "String",
    ): TestClass {
        return TestClass(
            simpleName = "Book",
            qualifiedName = "demo.Book",
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
