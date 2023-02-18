package org.babyfish.jimmer.sql.kt.model.calc

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.avg
import org.babyfish.jimmer.sql.kt.ast.expression.coalesce
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.model.Book
import org.babyfish.jimmer.sql.kt.model.id
import org.babyfish.jimmer.sql.kt.model.price
import org.babyfish.jimmer.sql.kt.model.store
import java.math.BigDecimal
import java.sql.Connection

class BookStoreAvgPriceResolver(
    private val sqlClient: KSqlClient
): KTransientResolver<Long, BigDecimal> {

    override fun resolve(ids: Collection<Long>): Map<Long, BigDecimal> =
        sqlClient
            .createQuery(Book::class) {
                where(table.store.id valueIn ids)
                groupBy(table.store.id)
                select(
                    table.store.id,
                    avg(table.price).coalesce(BigDecimal.ZERO)
                )
            }
            .execute(KTransientResolver.currentConnection)
            .associateBy(
                {it._1}
            ) {
                it._2
            }
}