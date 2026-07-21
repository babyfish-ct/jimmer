package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.sql.runtime.ScalarProvider

class PointProvider : ScalarProvider<Point, String> {

    override fun toScalar(sqlValue: String): Point = READER.read(sqlValue)

    override fun toSql(scalarValue: Point): String = WRITER.writeAsString(scalarValue)

    override fun isJsonScalar(): Boolean = true

    companion object {
        @JvmStatic
        private val READER = jsonCodec().readerFor(Point::class.java)

        @JvmStatic
        private val WRITER = jsonCodec().writer()
    }
}