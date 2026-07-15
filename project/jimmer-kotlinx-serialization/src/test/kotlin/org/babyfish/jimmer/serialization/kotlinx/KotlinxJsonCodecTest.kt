package org.babyfish.jimmer.serialization.kotlinx

import kotlinx.serialization.Serializable
import org.babyfish.jimmer.json.codec.JsonCodec
import org.babyfish.jimmer.json.codec.JsonCodecFamily
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.Serialized
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class KotlinxJsonCodecTest {

    private val codec = KotlinxJsonCodec()

    @Test
    fun `reader and typed writer support serializable class`() {
        val payload = Payload(
            id = 1,
            name = "jimmer",
            tags = listOf("orm", "json")
        )

        val json = codec.writerFor(Payload::class.java).writeAsString(payload)

        assertEquals("""{"id":1,"name":"jimmer","tags":["orm","json"]}""", json)
        assertEquals(payload, codec.readerFor(Payload::class.java).read(json))
        assertEquals(JsonCodecFamily.KOTLINX_SERIALIZATION, codec.family())
    }

    @Test
    fun `untyped writer supports serializable class`() {
        val payload = Payload(
            id = 3,
            name = "cache",
            tags = listOf("serialized")
        )

        val json = codec.writer().writeAsString(payload)

        assertEquals("""{"id":3,"name":"cache","tags":["serialized"]}""", json)
        assertEquals(payload, codec.readerFor(Payload::class.java).read(json))
    }

    @Test
    fun `generic readers support list and map`() {
        val payloads = codec
            .readerForListOf(Payload::class.java)
            .read("""[{"id":1,"name":"a","tags":[]},{"id":2,"name":"b","tags":["x"]}]""")
        val map = codec
            .readerForMapOf(Payload::class.java)
            .read("""{"first":{"id":1,"name":"a","tags":[]}}""")

        assertEquals(listOf("a", "b"), payloads.map { it.name })
        assertEquals(Payload(1, "a", emptyList()), map["first"])
    }

    @Test
    fun `tree reader exposes scalar casts and fields`() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val node = codec.treeReader().read("""{"id":"$id","count":3,"enabled":true}""")
        val fields = node.fieldsIterator().asSequence().map { it.key }.toList()

        assertEquals(id, node["id"].castTo(UUID::class.java))
        assertEquals(3, node["count"].castTo(Int::class.java))
        assertEquals(true, node["enabled"].castTo(Boolean::class.java))
        assertEquals(listOf("id", "count", "enabled"), fields)
    }

    @Test
    fun `service loader can make kotlinx codec the default json codec`() {
        assertEquals(JsonCodecFamily.KOTLINX_SERIALIZATION, JsonCodec.jsonCodec().family())
    }

    @Test
    fun `sql serialized type scalar provider uses kotlinx codec`() {
        val sqlClient = JSqlClient
            .newBuilder()
            .setDefaultSerializedTypeJsonCodec(codec)
            .build() as JSqlClientImplementor
        val provider: ScalarProvider<SerializedPayload, String> =
            sqlClient.getScalarProvider(SerializedPayload::class.java)
        val payload = SerializedPayload("sql", listOf(1, 2, 3))

        val sqlValue = provider.toSql(payload)

        assertEquals("""{"name":"sql","scores":[1,2,3]}""", sqlValue)
        assertEquals(payload, provider.toScalar(sqlValue))
    }

    @Serializable
    data class Payload(
        val id: Int,
        val name: String,
        val tags: List<String>
    )

    @Serialized
    @Serializable
    data class SerializedPayload(
        val name: String,
        val scores: List<Int>
    )
}
