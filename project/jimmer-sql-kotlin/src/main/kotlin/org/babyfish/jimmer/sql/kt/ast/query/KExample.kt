package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.StringExpression
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.meta.SingleColumn
import org.babyfish.jimmer.sql.meta.Storage
import kotlin.reflect.KProperty1

fun <E: Any> example(obj: E, block: (KExample.Dsl<E>.() -> Unit)? = null): KExample<E> {
    if (obj !is ImmutableSpi) {
        throw IllegalArgumentException("obj is not immutable object")
    }
    for (prop in obj.__type().props.values) {
        if (obj.__isLoaded(prop.id) && prop.getStorage<Storage>() == null) {
            throw IllegalArgumentException(
                "The property \"$prop\" of example cannot be loaded because it is not mapped by database columns"
            )
        }
    }
    val likeOpMap = if (block == null) {
        emptyMap()
    } else {
        val dsl = KExample.Dsl<E>()
        dsl.block()
        dsl.likeOpMap
    }
    return KExample(obj, likeOpMap)
}

class KExample<E: Any> internal constructor(
    private val spi: ImmutableSpi,
    private val likeOpMap: Map<ImmutableProp, LikeOp>
) {
    companion object {

        @JvmStatic
        private fun expressionOf(table: Table<*>, prop: ImmutableProp): Expression<Any?> =
            if (prop.isReference(TargetLevel.ENTITY)) {
                val joinedExpr = table.join<Table<*>>(prop.name)
                joinedExpr.get(prop.targetType.idProp.name)
            } else {
                table.get(prop.name)
            }

        @JvmStatic
        private fun valueOf(spi: ImmutableSpi, prop: ImmutableProp): Any? =
            spi.__get(prop.id)?.let {
                if (prop.isReference(TargetLevel.ENTITY)) {
                    (it as ImmutableSpi).__get(prop.targetType.idProp.name)
                } else {
                    it
                }
            }
    }

    internal val type: ImmutableType
        get() = spi.__type()

    internal fun applyTo(query: MutableRootQueryImpl<*>) {
        val table = query.getTable<Table<*>>()
        for (prop in spi.__type().props.values) {
            if (spi.__isLoaded(prop.id)) {
                val expr = expressionOf(table, prop)
                val value = valueOf(spi, prop)
                if (value === null) {
                    query.where(expr.isNull)
                } else {
                    val likeOp = likeOpMap[prop]
                    when {
                        likeOp == null -> query.where(expr.eq(value))
                        likeOp.insensitive -> query.where(
                            (expr as StringExpression).ilike(value as String, likeOp.mode)
                        )
                        else -> query.where(
                            (expr as StringExpression).like(value as String, likeOp.mode)
                        )
                    }
                }
            }
        }
    }

    @DslScope
    class Dsl<E: Any> {

        private val _likeOpMap = mutableMapOf<ImmutableProp, LikeOp>()

        fun like(prop: KProperty1<E, String?>, likeMode: LikeMode = LikeMode.ANYWHERE) {
            _likeOpMap[prop.toImmutableProp()] = LikeOp(false, likeMode)
        }

        fun ilike(prop: KProperty1<E, String?>, likeMode: LikeMode = LikeMode.ANYWHERE) {
            _likeOpMap[prop.toImmutableProp()] = LikeOp(true, likeMode)
        }

        internal val likeOpMap: Map<ImmutableProp, LikeOp>
            get() = _likeOpMap
    }

    internal data class LikeOp(
        val insensitive: Boolean,
        val mode: LikeMode
    )
}