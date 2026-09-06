package org.babyfish.jimmer.ksp.dto

import com.tschuchort.compiletesting.*
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.*

@OptIn(ExperimentalCompilerApi::class)
class PropConfigTest : AbstractTest() {

    @Test
    fun `unsupported where literals report the property and DTO position`() {
        for (prop in listOf("bytes", "tags", "attrs", "location", "createdAt", "custom", "keyedParentId")) {
            val result = prepare("!where($prop = 10)").compile()
            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
            assertContains(result.messages, "Entity.dto:4 : The \"!where\" in DTO must be simple predicate")
            assertContains(result.messages, "org.example.Item.")
            assertContains(result.messages, "    !where($prop = 10)\n           ^")
            assertFalse(result.messages.contains("IllegalStateException"), result.messages)
        }
    }

    @Test
    fun `association id lists are rejected before code generation`() {
        for (prop in listOf("peerIds", "peerKeys")) {
            for (config in listOf("!orderBy($prop)", "!where($prop = 10)", "!where($prop is null)")) {
                val result = prepare(config).compile()
                assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
                assertContains(result.messages, "Entity.dto:4 :")
                assertContains(result.messages, "org.example.Item.$prop")
                assertContains(result.messages, "join is forbidden by fetcher field predicate")
                assertContains(result.messages, "    $config\n")
                assertContains(result.messages, "^")
            }
        }
    }

    @Test
    fun `custom reference id view names compile in fetcher configurations`() {
        for ((parentId, keyedParentId) in listOf("parentKey" to "keyedParentKey", "parentId" to "keyedParentId")) {
            val result = prepare(
                "!orderBy($parentId asc, $keyedParentId.x desc)\n" +
                    "    !where($parentId = 1 and $keyedParentId.x = 2 and $keyedParentId is not null)"
            ).compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        }
    }

    @Test
    fun `ordering and nullity do not require a DTO literal type`() {
        val result = prepare(
            "!orderBy(bytes, tags, attrs, location, location.x, createdAt, custom, keyedParentId, keyedParentId.x)\n" +
                "    !where(attrs is null and location is not null and keyedParentId is not null)"
        ).compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    private fun prepare(config: String): KotlinCompilation {
        val compilation = createCompilation().apply {
            sources = MODEL.map { (name, code) ->
                SourceFile.kotlin(
                    "$name.kt",
                    "package org.example\nimport org.babyfish.jimmer.sql.*\nimport java.time.LocalDate\n" + code.trimIndent()
                )
            }
        }
        compilation.workingDir.resolve("src/main/dto").mkdirs()
        compilation.workingDir.resolve("src/main/dto/Entity.dto").writeText(
            "export org.example.Parent\n\nParentView {\n    $config\n    items { id }\n}\n"
        )
        return compilation
    }

    companion object {
        private val MODEL = mapOf(
            "Parent" to """
            @Entity
            interface Parent {
                @Id val id: Long
                @OneToMany(mappedBy = "parent") val items: List<Item>
            }
            """,
            "Item" to """
            @Entity
            interface Item {
                @Id val id: Long
                @ManyToOne val parent: Parent
                @IdView("parent") val parentKey: Long
                val bytes: ByteArray
                val tags: List<String>
                @Serialized val attrs: Map<String, String>?
                val location: Point
                val createdAt: LocalDate
                val custom: CustomScalar
                @ManyToMany val peers: List<Item>
                @IdView("peers") val peerIds: List<Long>
                @ManyToMany val keyedPeers: List<KeyedItem>
                @IdView("keyedPeers") val peerKeys: List<Point>
                @ManyToOne val keyedParent: KeyedItem
                @IdView("keyedParent") val keyedParentKey: Point
            }
            data class CustomScalar(val value: String)
            """,
            "KeyedItem" to """
            @Entity
            interface KeyedItem {
                @Id val key: Point
            }
            """,
            "Point" to """
            @Embeddable
            interface Point {
                val x: Int
                val y: Int
            }
            """
        )
    }
}
