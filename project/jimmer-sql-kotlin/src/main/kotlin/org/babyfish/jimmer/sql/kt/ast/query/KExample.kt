package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.View
import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.StringExpression
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.meta.FormulaTemplate
import kotlin.reflect.KProperty1

fun <E: Any> example(obj: E, block: (KExample.Dsl<E>.() -> Unit)? = null): KExample<E> {
    if (obj is View<*>) {
        throw IllegalArgumentException(
            "entity cannot be view, please call `viewExample`"
        )
    }
    if (obj !is ImmutableSpi) {
        throw IllegalArgumentException("obj is not immutable object")
    }
    return if (block == null) {
        KExample(obj, KExample.MatchMode.NOT_EMPTY, false, emptyMap())
    } else {
        val dsl = KExample.Dsl<E>()
        dsl.block()
        KExample(obj, dsl.matchMode, dsl.trim, dsl.propDataMap)
    }
}

fun <E: Any> viewExample(view: View<E>, block: (KExample.Dsl<E>.() -> Unit)? = null): KExample<E> =
    example(view.toEntity(), block)

class KExample<E: Any> internal constructor(
    private val spi: ImmutableSpi,
    private val matchMode: MatchMode,
    private val trim: Boolean,
    private val propDataMap: Map<ImmutableProp, PropData>
) {
    enum class MatchMode {
        NOT_EMPTY,
        NOT_NULL,
        NULLABLE
    }

    companion object {

        @JvmStatic
        private fun expressionOf(table: Table<*>, prop: ImmutableProp, outer: Boolean): Expression<Any?> =
            if (prop.isReference(TargetLevel.PERSISTENT)) {
                val joinedExpr =
                    if (outer) {
                        table.join<Table<*>>(prop.name, JoinType.LEFT)
                    }
                    else {
                        table.join<Table<*>>(prop.name, JoinType.INNER)
                    }
                joinedExpr.get(prop.targetType.idProp.name)
            } else {
                table.get(prop.name)
            }

        @JvmStatic
        private fun valueOf(spi: ImmutableSpi, prop: ImmutableProp): Any? =
            spi.__get(prop.id)?.let {
                if (prop.isReference(TargetLevel.PERSISTENT)) {
                    (it as ImmutableSpi).__get(prop.targetType.idProp.name)
                } else {
                    it
                }
            }
    }

    internal val type: ImmutableType
        get() = spi.__type()

    internal fun toPredicate(table: Table<*>): Predicate? {
        val predicates = mutableListOf<Predicate?>()
        for (prop in spi.__type().props.values) {
            if (spi.__isLoaded(prop.id) &&
                (prop.isColumnDefinition || prop.sqlTemplate is FormulaTemplate)) {
                val value = valueOf(spi, prop)
                val expr = expressionOf(table, prop, value == null)
                val data = propDataMap[prop]
                val matchMode = data?.matchMode ?: matchMode
                predicates += when {
                    value === null ->
                        if (matchMode == MatchMode.NULLABLE) expr.isNull else null
                    value is String -> {
                        val str = if (data?.trim ?: trim) {
                            value.trim()
                        } else {
                            value
                        }
                        val insensitive = data?.insensitive ?: false
                        val likeMode = data?.likeMode ?: LikeMode.EXACT
                        when {
                            str.isEmpty() && matchMode == MatchMode.NOT_EMPTY -> null
                            insensitive -> (expr as StringExpression).ilike(str, likeMode)
                            likeMode != LikeMode.EXACT -> (expr as StringExpression).like(str, likeMode)
                            else -> expr.eq(str)
                        }
                    }
                    value is Number ->
                        if (value.toInt() == 0 && data?.ignoredZero ?: false) {
                            null
                        } else {
                            expr.eq(value)
                        }
                    else ->
                        expr.eq(value)
                }
            }
        }
        return Predicate.and(*predicates.toTypedArray())
    }

    @DslScope
    class Dsl<E: Any> {

        private var _matchMode: MatchMode = MatchMode.NOT_EMPTY

        private var _trim: Boolean = false

        private val _propDataMap = mutableMapOf<ImmutableProp, PropData>()

        fun match(mode: MatchMode) {
            _matchMode = mode
        }

        fun trim() {
            _trim = true
        }

        fun match(prop: KProperty1<E, *>, mode: MatchMode) {
            val immutableProp = prop.toImmutableProp()
            val data = _propDataMap[immutableProp]
                ?.copy(matchMode = mode)
                ?: PropData(matchMode = mode)
            _propDataMap[immutableProp] = data
        }

        fun trim(prop: KProperty1<E, String?>) {
            val immutableProp = prop.toImmutableProp()
            val data = _propDataMap[immutableProp]
                ?.copy(trim = true)
                ?: PropData(trim = true)
            _propDataMap[immutableProp] = data
        }

        fun ignoreZero(prop: KProperty1<E, Number?>) {
            val immutableProp = prop.toImmutableProp()
            val data = _propDataMap[immutableProp]
                ?.copy(ignoredZero = true)
                ?: PropData(ignoredZero = true)
            _propDataMap[immutableProp] = data
        }

        fun like(prop: KProperty1<E, String?>, likeMode: LikeMode = LikeMode.ANYWHERE) {
            val immutableProp = prop.toImmutableProp()
            val data = _propDataMap[immutableProp]
                ?.copy(insensitive = false, likeMode = likeMode)
                ?: PropData(insensitive = false, likeMode = likeMode)
            _propDataMap[immutableProp] = data
        }

        fun ilike(prop: KProperty1<E, String?>, likeMode: LikeMode = LikeMode.ANYWHERE) {
            val immutableProp = prop.toImmutableProp()
            val data = _propDataMap[immutableProp]
                ?.copy(insensitive = true, likeMode = likeMode)
                ?: PropData(insensitive = true, likeMode = likeMode)
            _propDataMap[immutableProp] = data
        }

        internal val matchMode: MatchMode
            get() = _matchMode

        internal val trim: Boolean
            get() = _trim

        internal val propDataMap: Map<ImmutableProp, PropData>
            get() = _propDataMap
    }

    internal data class PropData(
        val matchMode: MatchMode? = null,
        val trim: Boolean = false,
        val ignoredZero: Boolean = false,
        val insensitive: Boolean = false,
        val likeMode: LikeMode = LikeMode.EXACT
    )
}