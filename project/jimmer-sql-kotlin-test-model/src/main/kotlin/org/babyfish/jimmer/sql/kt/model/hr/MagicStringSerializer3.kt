package org.babyfish.jimmer.sql.kt.model.hr

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer

class MagicStringSerializer3 : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String, gen: JsonGenerator, provider: SerializationContext) {
        gen.writeString(MagicStringCodec.serialize(value))
    }
}