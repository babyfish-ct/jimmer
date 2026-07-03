package org.babyfish.jimmer.sql.kt.ast.query.specification

import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.client.ApiIgnore
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification
import org.babyfish.jimmer.sql.ast.query.specification.SpecificationArgs
import org.babyfish.jimmer.sql.ast.table.Table

@ApiIgnore
interface KSpecification<E : Any> : Specification<E> {

    fun applyTo(args: KSpecificationArgs<E>)

    companion object {

        @JvmStatic
        fun <E : Any> and(vararg specifications: KSpecification<out E>?): KSpecification<E>? =
            compose(Operator.AND, specifications.asList())

        @JvmStatic
        fun <E : Any> and(specifications: Iterable<KSpecification<out E>?>): KSpecification<E>? =
            compose(Operator.AND, specifications)

        @JvmStatic
        fun <E : Any> or(vararg specifications: KSpecification<out E>?): KSpecification<E>? =
            compose(Operator.OR, specifications.asList())

        @JvmStatic
        fun <E : Any> or(specifications: Iterable<KSpecification<out E>?>): KSpecification<E>? =
            compose(Operator.OR, specifications)

        @JvmStatic
        fun <E : Any> not(specification: KSpecification<out E>?): KSpecification<E>? =
            negate(specification)
    }
}

fun <E : Any> KSpecification<E>.toJavaSpecification(): JSpecification<E, Table<E>> =
    object : JSpecification<E, Table<E>> {

        override fun entityType(): Class<E> =
            this@toJavaSpecification.entityType()

        override fun applyTo(args: SpecificationArgs<E, Table<E>>) {
            this@toJavaSpecification.applyTo(KSpecificationArgs(args.applier))
        }
    }
