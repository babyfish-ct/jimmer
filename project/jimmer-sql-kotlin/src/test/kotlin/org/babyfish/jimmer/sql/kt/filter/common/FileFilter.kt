package org.babyfish.jimmer.sql.kt.filter.common

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.exists
import org.babyfish.jimmer.sql.kt.ast.table.sourceId
import org.babyfish.jimmer.sql.kt.ast.table.targetId
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.model.filter.File
import org.babyfish.jimmer.sql.kt.model.filter.id

open class FileFilter : KFilter<File> {

    override fun filter(args: KFilterArgs<File>) {
        args.where(
            exists(
                args.wildSubQueries.forList(File::users) {
                    where(table.sourceId eq parentTable.id)
                    where(table.targetId eq currentUserId)
                }
            )
        )
    }

    companion object {

        private val USER_ID_LOCAL = ThreadLocal<Long>()

        val currentUserId: Long
            get() = USER_ID_LOCAL.get() ?: error("No user id")

        fun <T> withUser(userId: Long, block: () -> T): T {
            val oldUserId = USER_ID_LOCAL.get()
            return if (oldUserId == userId) {
                block()
            } else {
                USER_ID_LOCAL.set(userId)
                try {
                    block()
                } finally {
                    if (oldUserId != null) {
                        USER_ID_LOCAL.set(oldUserId)
                    } else {
                        USER_ID_LOCAL.remove()
                    }
                }
            }
        }
    }
}