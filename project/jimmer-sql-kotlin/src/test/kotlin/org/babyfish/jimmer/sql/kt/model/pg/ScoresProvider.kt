package org.babyfish.jimmer.sql.kt.model.pg

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.postgresql.util.PGobject

class ScoresProvider : ScalarProvider<Map<Long, Int>, PGobject>() {

    override fun toScalar(sqlValue: PGobject): Map<Long, Int> =
        MAPPER.readValue(sqlValue.value, TYPE_REFERENCE)

    override fun toSql(scalarValue: Map<Long, Int>): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = MAPPER.writeValueAsString(scalarValue)
        }

    override fun getHandledProps(): Collection<ImmutableProp> =
        listOf(JsonWrapper::scores.toImmutableProp())

    companion object {

        @JvmStatic
        private val MAPPER = jacksonObjectMapper()

        @JvmStatic
        private val TYPE_REFERENCE = object: TypeReference<Map<Long, Int>>() {}
    }
}