package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.MutationResultItem

/**
 * Result of saving one root entity and its supplied associations.
 *
 * [originalEntity] is the immutable input; [modifiedEntity] contains the result of saving it.
 * [isAccepted] reports the root row's logical outcome, whereas [isModified] reports a change
 * of entity instance. Neither flag is derived from affected-row counts.
 * Counts inherited from [KMutationResult] include changes to associated entities and association tables.
 */
interface KSimpleSaveResult<E: Any> : KMutationResult, MutationResultItem<E> {

    /**
     * Whether [affectedRowCountMap] contains any affected table, including association tables.
     * This does not indicate whether the root row was accepted; use [isAccepted] for that.
     */
    val isRowAffected: Boolean
        get() = affectedRowCountMap.isNotEmpty()

    /** A save result that also exposes [modifiedEntity] converted to a view. */
    interface View<E: Any, V: org.babyfish.jimmer.View<E>> : KSimpleSaveResult<E> {

        /** The view of the resulting entity, with the same save outcome as this result. */
        val modifiedView: V
    }
}
