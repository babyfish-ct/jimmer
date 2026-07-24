package org.babyfish.jimmer.sql.kt.model.hr

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class MagicStringDeserializer : StdDeserializer<String>(String::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String {
        return MagicStringCodec.deserialize(ctxt.readValue(p, String::class.java))
    }
}