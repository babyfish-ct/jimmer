package org.babyfish.jimmer.sql.kt.ast.query.specification

interface KSpecification<E: Any> {

    fun applyTo(args: KSpecificationArgs<E>)
}