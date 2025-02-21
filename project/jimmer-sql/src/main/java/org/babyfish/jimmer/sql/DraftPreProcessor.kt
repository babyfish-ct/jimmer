package org.babyfish.jimmer.sql

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.meta.KeyMatcher

/**
 * Before saving draft, give user a chance to modify it.
 *
 * This interface is very similar to another
 * interface [DraftInterceptor],
 * the differences between the two are as follows:
 *
 * -   **This interface**:
 *
 *     Jimmer does not check whether the data being
 *     saved exists and unconditionally calls it,
 *     setting default values for unloaded properties
 *     of the object being saved.
 *     Because it doesn't perform any checks, it has
 *     higher performance but less functionality.
 *
 * -   [DraftInterceptor]:
 *     Jimmer will give up database-level upsert capabilities
 *     and execute a query to check if the data being saved
 *     exists *(QueryReason.INTERCEPTOR)*. Therefore, developers
 *     can explicitly know whether the data being saved exists
 *     and whether it will be inserted or update. As a result,
 *     it has more functionality but lower performance.
 *
 * @param <D> Draft type
 *
 * @see DraftInterceptor
 */
interface DraftPreProcessor<D: Draft> {

    fun beforeSave(draft: D)

    /**
     * Jimmer will call this method if the id-only
     * object is treated as a short association
     * *(set to true by the save command's
     * `setIdOnlyAsReference` and `setIdOnlyAsReferenceAll`,
     * the default value is `true`)*.
     * Otherwise, this method is **never** called.
     *
     * You can override this method to tell jimmer
     * whether to ignore modifications to drafts
     * of id-only objects, the default value is `false`.
     *
     * If multiple DraftPreProcessors act on an id-only
     * object, and any `DraftPreProcessor` intends to
     * ignore the modification operation, the modification
     * operation will be ignored finally.
     *
     * The return value of this method must be stable,
     * and different calls must return the same return value.
     */
    fun ignoreIdOnly(): Boolean = false

    /**
     * Jimmer will call this method if the key-only
     * object is treated as a short association
     * *(set to true by the save command's
     * `setKeyOnlyAsReference` and `setKeyOnlyAsReferenceAll`,
     * the default value is `false`)*.
     * Otherwise, this method is **never** called.
     *
     * You can override this method to tell jimmer
     * whether to ignore modifications to drafts
     * of key-only objects, the default value is `false`.
     *
     * If multiple DraftPreProcessors act on a key-only
     * object, and any `DraftPreProcessor` intends to
     * ignore the modification operation, the modification
     * operation will be ignored finally.
     *
     * The return value of this method must be stable,
     * and different calls must return the same return value.
     */
    fun ignoreKeyOnly(group: KeyMatcher.Group) = false
}