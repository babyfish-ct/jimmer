package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.avg
import org.babyfish.jimmer.sql.kt.ast.expression.coalesce
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import java.math.BigDecimal
import java.sql.Connection

class BookStoreAvgPriceResolver(
    private val sqlClient: KSqlClient
): KTransientResolver<Long, BigDecimal> {

    override fun resolve(
        ids: Collection<Long>,
        con: Connection
    ): Map<Long, BigDecimal> =
        sqlClient
            .createQuery(Book::class) {
                where(table.store.id valueIn ids)
                groupBy(table.store.id)
                select(
                    table.store.id,
                    avg(table.price).coalesce(BigDecimal.ZERO)
                )
            }
            .execute(con)
            .associateBy(
                {it._1}
            ) {
                it._2
            }
}