package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.KRemoteRef

internal abstract class KRemoteRefImpl<E: Any>(
    protected val javaTable: Table<*>
) : KRemoteRefImplementor<E> {

    class NonNull<E: Any>(
        javaTable: Table<*>
    ) : KRemoteRefImpl<E>(javaTable), KRemoteRef.NonNull<E> {

        override fun <X : Any> id(): KNonNullPropExpression<X> =
            NonNullPropExpressionImpl(
                javaTable.get<X>(javaTable.immutableType.idProp) as PropExpressionImpl<X>
            )
    }

    class Nullable<E: Any>(
        javaTable: Table<*>
    ) : KRemoteRefImpl<E>(javaTable), KRemoteRef.Nullable<E> {

        override fun <X : Any> id(): KNullablePropExpression<X> =
            NullablePropExpressionImpl<X>(
                javaTable.get<X>(javaTable.immutableType.idProp) as PropExpressionImpl<X>
            )
    }
}