package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.sql.EnumItem

enum class DependencyScope {

    @EnumItem(name = "C")
    COMPILE,

    @EnumItem(name = "R")
    RUNTIME,

    @EnumItem(name = "P")
    PROVIDED,

    @EnumItem(name = "T")
    TEST,

    @EnumItem(name = "S")
    SYSTEM,

    @EnumItem(name = "I")
    IMPORT
}