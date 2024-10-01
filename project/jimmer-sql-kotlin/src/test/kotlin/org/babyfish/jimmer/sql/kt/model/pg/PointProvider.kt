package org.babyfish.jimmer.sql.kt.model.pg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.sql.runtime.ScalarProvider

class PointProvider : ScalarProvider<Point, String> {

    override fun toScalar(sqlValue: String): Point =
        MAPPER.readValue(sqlValue, Point::class.java)

    override fun toSql(scalarValue: Point): String =
        MAPPER.writeValueAsString(scalarValue)

    override fun isJsonScalar(): Boolean = true

    companion object {

        @JvmStatic
        private val MAPPER = jacksonObjectMapper()
    }
}