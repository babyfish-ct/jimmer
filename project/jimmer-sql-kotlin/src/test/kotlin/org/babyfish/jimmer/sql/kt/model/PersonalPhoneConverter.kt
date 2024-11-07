package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.jackson.Converter

class PersonalPhoneConverter : Converter<String, String> {
    override fun output(value: String): String =
        "${value.substring(0, 3)}****${value.substring(7)}"
}
