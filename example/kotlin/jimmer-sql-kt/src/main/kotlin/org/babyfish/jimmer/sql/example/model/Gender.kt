package org.babyfish.jimmer.sql.example.model

import org.babyfish.jimmer.sql.EnumItem

enum class Gender {

    @EnumItem(name = "M") // ❶
    MALE,

    @EnumItem(name = "F") // ❷
    FEMALE
}

/*----------------Documentation Links----------------
https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/enum
---------------------------------------------------*/