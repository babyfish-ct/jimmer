package org.babyfish.jimmer.sql.kt.model.calc

import org.babyfish.jimmer.sql.TypedTransientResolver
import org.babyfish.jimmer.sql.kt.ExperimentalTypedTransientResolver
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.KTypedTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.version

@OptIn(ExperimentalTypedTransientResolver::class)
class BookStoreNameWithVersionResolver(
    private val sqlClient: KSqlClient
) : KTypedTransientResolver<BookStore, Long, String> {

    override fun resolve(ids: Collection<Long>): Map<Long, String> =
        sqlClient.createQuery(BookStore::class) {
            where(table.id valueIn ids)
            select(
                table.id,
                table.name,
                table.version
            )
        }.execute(KTransientResolver.currentConnection)
            .associateBy({ it._1 }) {
                "${it._2}#${it._3}"
            }

    override fun resolve(
        ids: Collection<Long>,
        context: TypedTransientResolver.Context<BookStore>
    ): Map<Long, String> {
        val idSet = ids.toHashSet()
        return context.content
            .filter { it.id in idSet }
            .associateBy({ it.id }) {
                "${it.name}#${it.version}"
            }
    }
}
