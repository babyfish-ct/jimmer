package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult
import org.babyfish.jimmer.sql.ast.mutation.MutationResultItem

interface KBatchSaveResult<E: Any> : KMutationResult {

    val items: List<MutationResultItem<E>>

    interface View<E: Any, V: org.babyfish.jimmer.View<E>> : KBatchSaveResult<E> {

        val viewItems: List<BatchSaveResult.View.ViewItem<E, V>>
    }
}