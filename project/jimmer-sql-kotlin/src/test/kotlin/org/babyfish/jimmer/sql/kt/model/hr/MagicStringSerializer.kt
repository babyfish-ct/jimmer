package org.babyfish.jimmer.sql.kt.model.hr

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class MagicStringSerializer : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String, gen: JsonGenerator, provider: SerializerProvider) {
        val builder = StringBuilder()
        for (c in value) {
            builder.append(c + 1)
        }
        gen.writeString(builder.toString())
    }
}