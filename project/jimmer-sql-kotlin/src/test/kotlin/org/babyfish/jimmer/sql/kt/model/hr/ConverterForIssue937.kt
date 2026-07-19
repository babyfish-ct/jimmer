package org.babyfish.jimmer.sql.kt.model.hr

import org.babyfish.jimmer.jackson.Converter

class ConverterForIssue937 : Converter<String, String> {
    override fun output(value: String): String = value
    override fun input(jsonValue: String): String = jsonValue
}