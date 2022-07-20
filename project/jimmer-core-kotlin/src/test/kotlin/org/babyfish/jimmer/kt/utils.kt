package org.babyfish.jimmer.kt

fun String.toSimpleJson() =
    replace("\r", "")
        .replace("\n", "")
        .replace("--->", "")