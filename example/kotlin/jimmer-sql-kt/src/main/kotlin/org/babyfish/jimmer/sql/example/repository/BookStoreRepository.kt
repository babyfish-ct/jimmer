package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.sql.kt.ast.expression.*
import java.math.BigDecimal

interface BookStoreRepository : KRepository<BookStore, Long> {

    fun findIdAndAvgBookPrice(ids: Collection<Long>): List<Tuple2<Long, BigDecimal>> =
        sql.createQuery(BookStore::class) {
            where(table.id valueIn ids)
            groupBy(table.id)
            select(
                table.id,
                avg(table.asTableEx().`books?`.price).coalesce(BigDecimal.ZERO)
            )
        }.execute()

    fun findIdAndNewestBookId(ids: Collection<Long>): List<Tuple2<Long, Long>> =
        sql.createQuery(BookStore::class) {
            where(
                tuple(
                    table.asTableEx().books.name,
                    table.asTableEx().books.edition
                ) valueIn subQuery(Book::class) {
                    where(table.store.id valueIn ids)
                    groupBy(table.name)
                    select(
                        table.name,
                        max(table.edition).asNonNull()
                    )
                }
            )
            select(
                table.id,
                table.asTableEx().books.id
            )
        }.execute()
}