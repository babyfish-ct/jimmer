package org.babyfish.jimmer.sql.kt.model.calc

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.asNonNull
import org.babyfish.jimmer.sql.kt.ast.expression.max
import org.babyfish.jimmer.sql.kt.ast.expression.tuple
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.model.*

class BookStoreNewestBooksResolver(
    private val sqlClient: KSqlClient
) : KTransientResolver<Long, List<Long>> {

    override fun resolve(ids: Collection<Long>): Map<Long, List<Long>> {
        val tuples = sqlClient.createQuery(BookStore::class) {
            val book = table.asTableEx().books
            where(
                tuple(book.name, book.edition) valueIn subQuery(Book::class) {
                    where(table.store.id valueIn ids)
                    groupBy(table.name)
                    select(table.name, max(table.edition).asNonNull())
                }
            )
            select(
                table.id,
                book.id
            )
        }.execute(KTransientResolver.currentConnection)
        return tuples.groupBy({it._1}) {
            it._2
        }
    }
}