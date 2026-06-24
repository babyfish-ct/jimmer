package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.runtime.ScalarProvider

class TagsProvider : ScalarProvider<List<String>, String> {

    override fun toScalar(sqlValue: String): List<String> = READER.read(sqlValue)

    override fun toSql(scalarValue: List<String>): String = WRITER.writeAsString(scalarValue)

    override fun isJsonScalar(): Boolean = true

    override fun getHandledProps(): Collection<ImmutableProp> =
        listOf(JsonWrapper::tags.toImmutableProp())

    companion object {
        @JvmStatic
        private val READER = jsonCodec().readerForListOf(String::class.java)

        @JvmStatic
        private val WRITER = jsonCodec().writer()
    }
}