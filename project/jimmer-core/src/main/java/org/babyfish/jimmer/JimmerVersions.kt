package org.babyfish.jimmer

fun JimmerVersion.asString(): String = "$major.$minor.$patch"

fun currentVersion(): String = JimmerVersion.asString()

fun isCurrentVersion(version: String): Boolean = version == currentVersion()