package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.table.KImplicitSubQueryTable
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KImplicitSubQueryTableImpl<E: Any>(
    javaTable: TableImplementor<E>,
) : KNonNullTableExImpl<E>(javaTable, null), KImplicitSubQueryTable<E> {

    override val subQueries: KSubQueries<E> =
        KSubQueriesImpl(
            MutableSubQueryImpl(
                javaTable.statement.sqlClient,
                immutableType,
            ),
            this
        )

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(
            MutableSubQueryImpl(
                javaTable.statement.sqlClient,
                immutableType
            ),
            this
        )
}