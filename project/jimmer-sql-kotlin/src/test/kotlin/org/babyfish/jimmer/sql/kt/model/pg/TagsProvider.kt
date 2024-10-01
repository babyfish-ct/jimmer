package org.babyfish.jimmer.sql.kt.model.pg

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.runtime.ScalarProvider

class TagsProvider : ScalarProvider<List<String>, String> {

    override fun toScalar(sqlValue: String): List<String> =
        MAPPER.readValue(sqlValue, TYPE_REFERENCE)

    override fun toSql(scalarValue: List<String>): String =
        MAPPER.writeValueAsString(scalarValue)

    override fun isJsonScalar(): Boolean = true

    override fun getHandledProps(): Collection<ImmutableProp> =
        listOf(JsonWrapper::tags.toImmutableProp())

    companion object {

        @JvmStatic
        private val MAPPER = jacksonObjectMapper()

        @JvmStatic
        private val TYPE_REFERENCE = object: TypeReference<List<String>>() {}
    }
}