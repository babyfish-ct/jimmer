package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.MutationResultItem

interface KSimpleSaveResult<E: Any> : KMutationResult, MutationResultItem<E> {

    val isRowAffected: Boolean
        get() = affectedRowCountMap.isNotEmpty()
}