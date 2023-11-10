package org.babyfish.jimmer.sql.kt.ast.query.specification

import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification
import org.babyfish.jimmer.sql.ast.query.specification.SpecificationArgs
import org.babyfish.jimmer.sql.ast.table.Table

interface KSpecification<E: Any> : Specification<E> {

    fun applyTo(args: KSpecificationArgs<E>)
}

fun <E: Any> KSpecification<E>.toJavaSpecification(): JSpecification<E, Table<E>> =
    object : JSpecification<E, Table<E>> {

        override fun entityType(): Class<E> =
            this@toJavaSpecification.entityType()

        override fun applyTo(args: SpecificationArgs<E, Table<E>>) {
            this@toJavaSpecification.applyTo(KSpecificationArgs(args.applier))
        }
    }
