package org.babyfish.jimmer.sql.kt.model.fetcher

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn

class Issue1434MessageUserDepartmentNamesResolver(
    private val sqlClient: KSqlClient
) : KTransientResolver<Long, String> {

    override fun resolve(ids: Collection<Long>): Map<Long, String> {
        val messages = sqlClient.createQuery(Issue1434Message::class) {
            where(table.id valueIn ids)
            select(
                table.fetchBy {
                    user {
                        departments {
                            name()
                        }
                    }
                }
            )
        }.execute(KTransientResolver.currentConnection)
        return messages.associate { message ->
            message.id to (
                message.user
                    ?.departments
                    ?.joinToString(",") { it.name }
                    ?: ""
            )
        }
    }
}
