package org.babyfish.jimmer.sql.kt.model.hr

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class MagicStringDeserializer : StdDeserializer<String>(String::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String {
        val value = ctxt.readValue(p, String::class.java)
        val builder = StringBuilder()
        for (c in value) {
            builder.append(c - 1)
        }
        return builder.toString()
    }
}