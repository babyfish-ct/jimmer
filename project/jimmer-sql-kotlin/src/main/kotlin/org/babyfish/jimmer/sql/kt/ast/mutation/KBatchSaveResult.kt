package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult
import org.babyfish.jimmer.sql.ast.mutation.MutationResultItem

/**
 * Result of saving multiple root entities and their supplied associations.
 * Row counts aggregate the whole command; acceptance and entity changes are reported per item.
 */
interface KBatchSaveResult<E: Any> : KMutationResult {

    /** One result per input entity, in input order, including rejected items. */
    val items: List<MutationResultItem<E>>

    /** A batch result that also exposes each resulting entity converted to a view. */
    interface View<E: Any, V: org.babyfish.jimmer.View<E>> : KBatchSaveResult<E> {

        /** View results in the same order as [items], preserving each item's acceptance state. */
        val viewItems: List<BatchSaveResult.View.ViewItem<E, V>>
    }
}
