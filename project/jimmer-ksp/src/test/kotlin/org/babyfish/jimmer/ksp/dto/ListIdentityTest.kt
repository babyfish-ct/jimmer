package org.babyfish.jimmer.ksp.dto

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class ListIdentityTest : AbstractTest() {

    @Test
    fun `generated Kotlin list getters setters and builders retain property ownership`() {
        val compilation = createCompilation()
        compilation.sources = listOf(SourceFile.kotlin("State.kt", """
            package org.example

            import org.babyfish.jimmer.Immutable
            import org.babyfish.jimmer.DraftObjects
            import org.babyfish.jimmer.ImmutableObjects
            import org.babyfish.jimmer.jackson.codec.JsonCodec
            import org.babyfish.jimmer.kt.new
            import org.babyfish.jimmer.meta.PropId
            import org.babyfish.jimmer.runtime.DraftSpi
            import java.util.Collections
            import kotlin.test.*

            @Immutable
            interface State {
                val left: List<String>
                val right: List<String>
                val optional: List<String>?
                val nodes: List<Node>
            }

            fun checkLists() {
                val shared = Collections.unmodifiableList(listOf("base"))
                val base = new(State::class).by { left = shared; right = shared; optional = null }
                assertSame(base.left, base.right)
                val changed = new(State::class).by(base) {
                    assertSame(left(), left())
                    val spi = this as DraftSpi
                    val named = PropId.byName("left")
                    val indexed = spi.__type().getProp("left").id
                    val cached = spi.__getListDraft(indexed)
                    assertSame(cached, spi.__getListDraft(named))
                    spi.__setListDraft(named, null)
                    assertNull(spi.__getListDraft(indexed))
                    spi.__setListDraft(named, cached)
                    assertSame(cached, spi.__getListDraft(indexed))
                    spi.__setListDraft(indexed, null)
                    assertNull(spi.__getListDraft(named))
                    spi.__setListDraft(indexed, cached)
                    assertSame(cached, spi.__getListDraft(named))
                    assertNull(spi.__getListDraft(PropId.byName("missing")))
                    assertFailsWith<IllegalArgumentException> { spi.__setListDraft(PropId.byName("missing"), null) }
                    assertNull(spi.__getListDraft(PropId.byIndex(1000)))
                    assertFailsWith<IllegalArgumentException> { spi.__setListDraft(PropId.byIndex(1000), null) }
                    left().add("new")
                    assertEquals(listOf("base"), right)
                    DraftObjects.unload(this, "left")
                    left = shared
                    assertEquals(listOf("base"), left())
                    left().add("last")
                }
                assertEquals(listOf("base", "last"), changed.left)
                assertEquals(listOf("base"), changed.right)
                assertNull(changed.optional)
                assertSame(base, new(State::class).by(base) { left().size })
                val built = StateDraft.Builder().left(shared).right(shared).optional(null).build()
                assertSame(shared, built.left)
                assertNull(built.optional)
                assertFailsWith<UnsupportedOperationException> { (built.left as MutableList<String>).clear() }
                val restored = JsonCodec.jsonCodec().readerFor(State::class.java).read("{\"left\":[\"a\"],\"optional\":null}")
                assertEquals(listOf("a"), restored.left)
                assertFalse(ImmutableObjects.isLoaded(restored, "right"))
                assertFailsWith<UnsupportedOperationException> { (restored.left as MutableList<String>).clear() }
                val first = new(Node::class).by { name = "first"; children = emptyList() }
                val second = new(Node::class).by { name = "second"; children = first.children }
                val tree = new(State::class).by {
                    nodes = listOf(first, second)
                    val spi = this as DraftSpi
                    val nodeList = nodes()
                    assertTrue((nodeList as Any) === spi.__getListDraft(PropId.byName("nodes")))
                    val node = nodeList[0] as DraftSpi
                    assertNull(node.__getListDraft(PropId.byName("name")))
                    assertFailsWith<IllegalArgumentException> { node.__setListDraft(PropId.byName("name"), null) }
                    nodes()[0].children().addBy { name = "child" }
                }
                assertEquals(1, tree.nodes[0].children.size)
                assertTrue(tree.nodes[1].children.isEmpty())
            }
        """.trimIndent()), SourceFile.kotlin("Node.kt", """
            package org.example
            import org.babyfish.jimmer.Immutable
            @Immutable
            interface Node {
                val name: String
                val children: List<Node>
            }
        """.trimIndent()))
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        result.classLoader.loadClass("org.example.StateKt").getMethod("checkLists").invoke(null)
    }
}
