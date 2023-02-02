package org.babyfish.jimmer.meta.impl.dto.ast.spi

interface BaseType {
    val name: String
    val qualifiedName: String
    val isEntity: Boolean
}