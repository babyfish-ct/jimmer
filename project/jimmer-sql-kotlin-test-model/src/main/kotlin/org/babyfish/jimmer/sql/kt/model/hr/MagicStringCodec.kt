package org.babyfish.jimmer.sql.kt.model.hr

object MagicStringCodec {
    fun serialize(value: String): String {
        val builder = StringBuilder()
        val size = value.length
        for (i in 0..<size) {
            builder.append((value[i].code + 1).toChar())
        }
        return builder.toString()
    }

    fun deserialize(value: String): String {
        val builder = StringBuilder()
        val size = value.length
        for (i in 0..<size) {
            builder.append((value[i].code - 1).toChar())
        }
        return builder.toString()
    }
}
