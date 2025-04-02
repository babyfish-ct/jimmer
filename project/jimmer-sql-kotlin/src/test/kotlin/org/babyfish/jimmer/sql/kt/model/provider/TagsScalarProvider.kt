package org.babyfish.jimmer.sql.kt.model.provider

import org.babyfish.jimmer.sql.runtime.ScalarProvider
import java.util.EnumSet

class TagsScalarProvider : ScalarProvider<EnumSet<Tag>, Int> {

    override fun toScalar(sqlValue: Int): EnumSet<Tag> {
        val tags = EnumSet.noneOf(Tag::class.java)
        for (tag in enumValues<Tag>()) {
            if (sqlValue and (1 shl tag.ordinal) != 0) {
                tags.add(tag)
            }
        }
        return tags
    }

    override fun toSql(scalarValue: EnumSet<Tag>): Int {
        var bits = 0
        for (tag in scalarValue) {
            bits = bits or (1 shl tag.ordinal)
        }
        return bits
    }
}