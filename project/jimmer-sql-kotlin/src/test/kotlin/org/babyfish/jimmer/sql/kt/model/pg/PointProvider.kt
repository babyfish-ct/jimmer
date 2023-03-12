package org.babyfish.jimmer.sql.kt.model.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.postgresql.util.PGobject

class PointProvider : ScalarProvider<Point, PGobject>() {

    override fun toScalar(sqlValue: PGobject): Point =
        MAPPER.readValue(sqlValue.value, Point::class.java)

    override fun toSql(scalarValue: Point): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = MAPPER.writeValueAsString(scalarValue)
        }

    companion object {

        @JvmStatic
        private val MAPPER = jacksonObjectMapper()
    }
}