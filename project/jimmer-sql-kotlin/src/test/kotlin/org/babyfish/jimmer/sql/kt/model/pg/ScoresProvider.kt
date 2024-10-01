package org.babyfish.jimmer.sql.kt.model.pg

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.runtime.ScalarProvider

class ScoresProvider : ScalarProvider<Map<Long, Int>, String> {

    override fun toScalar(sqlValue: String): Map<Long, Int> =
        MAPPER.readValue(sqlValue, TYPE_REFERENCE)

    override fun toSql(scalarValue: Map<Long, Int>): String =
        MAPPER.writeValueAsString(scalarValue)

    override fun isJsonScalar(): Boolean = true

    override fun getHandledProps(): Collection<ImmutableProp> =
        listOf(JsonWrapper::scores.toImmutableProp())

    companion object {

        @JvmStatic
        private val MAPPER = jacksonObjectMapper()

        @JvmStatic
        private val TYPE_REFERENCE = object: TypeReference<Map<Long, Int>>() {}
    }
}