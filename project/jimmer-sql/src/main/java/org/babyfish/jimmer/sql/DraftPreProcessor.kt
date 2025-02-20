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

    fun ignoreIdOnly(): Boolean = false

    fun ignoreKeyOnly(group: KeyMatcher.Group) = false
}