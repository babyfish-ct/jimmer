package org.babyfish.jimmer.dto.compiler.spi

interface BaseType {
    val name: String
    val qualifiedName: String
    val isEntity: Boolean
}