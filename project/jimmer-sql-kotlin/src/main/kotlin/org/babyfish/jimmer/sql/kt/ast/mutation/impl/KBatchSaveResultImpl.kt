package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult.Item
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult

internal class KBatchSaveResultImpl<E: Any>(
    javaResult: BatchSaveResult<E>
) : KMutationResultImpl(javaResult), KBatchSaveResult<E> {

    override val items: List<Item<E>> =
        javaResult.items
}