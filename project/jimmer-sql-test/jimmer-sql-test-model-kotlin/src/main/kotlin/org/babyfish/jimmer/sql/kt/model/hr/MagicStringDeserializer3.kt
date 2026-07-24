package org.babyfish.jimmer.sql.kt.model.hr

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class MagicStringDeserializer3 : StdDeserializer<String>(String::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String {
        return MagicStringCodec.deserialize(ctxt.readValue(p, String::class.java))
    }
}