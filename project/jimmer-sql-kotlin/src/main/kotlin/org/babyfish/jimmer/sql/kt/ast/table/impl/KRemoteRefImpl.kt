package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
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

        @Suppress("UNCHECKED_CAST")
        override fun <X : Any, EXP : KPropExpression<X>> id(): EXP =
            NonNullPropExpressionImpl<X>(
                javaTable.get(javaTable.immutableType.idProp.name)
            ) as EXP
    }

    class Nullable<E: Any>(
        javaTable: Table<*>
    ) : KRemoteRefImpl<E>(javaTable), KRemoteRef.Nullable<E> {

        @Suppress("UNCHECKED_CAST")
        override fun <X : Any, EXP : KPropExpression<X>> id(): EXP =
            NullablePropExpressionImpl<X>(
                javaTable.get(javaTable.immutableType.idProp.name)
            ) as EXP
    }
}