package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult.Item
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult

internal open class KBatchSaveResultImpl<E: Any>(
    javaResult: BatchSaveResult<E>
) : KMutationResultImpl(javaResult), KBatchSaveResult<E> {

    override val items: List<Item<E>> =
        javaResult.items

    internal class ViewImpl<E: Any, V:View<E>>(
        javaResult: BatchSaveResult.View<E, V>
    ) : KBatchSaveResultImpl<E>(javaResult), KBatchSaveResult.View<E, V> {

        @Suppress("UNCHECKED_CAST")
        override val viewItems: List<BatchSaveResult.View.ViewItem<E, V>>
            get() = (javaResult as BatchSaveResult.View<E, V>).viewItems
    }
}