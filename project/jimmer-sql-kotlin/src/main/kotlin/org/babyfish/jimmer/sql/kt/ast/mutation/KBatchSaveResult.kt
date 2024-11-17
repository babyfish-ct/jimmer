package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.MutationResultItem

interface KBatchSaveResult<E: Any> : KMutationResult {

    val items: List<MutationResultItem<E>>
}