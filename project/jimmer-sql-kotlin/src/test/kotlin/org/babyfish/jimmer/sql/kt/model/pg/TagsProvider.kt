package org.babyfish.jimmer.sql.kt.model.pg

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.postgresql.util.PGobject

class TagsProvider : ScalarProvider<List<String>, PGobject>() {

    override fun toScalar(sqlValue: PGobject): List<String> =
        MAPPER.readValue(sqlValue.value, TYPE_REFERENCE)

    override fun toSql(scalarValue: List<String>): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = MAPPER.writeValueAsString(scalarValue)
        }

    override fun getHandledProps(): Collection<ImmutableProp> =
        listOf(JsonWrapper::tags.toImmutableProp())

    companion object {

        @JvmStatic
        private val MAPPER = jacksonObjectMapper()

        @JvmStatic
        private val TYPE_REFERENCE = object: TypeReference<List<String>>() {}
    }
}