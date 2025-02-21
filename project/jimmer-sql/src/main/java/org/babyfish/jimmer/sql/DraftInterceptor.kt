package org.babyfish.jimmer.sql

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.meta.KeyMatcher
import org.babyfish.jimmer.meta.TypedProp

/**
 * Before saving draft, give user a chance to modify it.
 *
 * This interface is very similar to another
 * interface [DraftPreProcessor],
 * the differences between the two are as follows:
 *
 *  -  **This interface**:
 *
 *     Jimmer will give up database-level upsert capabilities
 *     and execute a query to check if the data being saved
 *     exists *(QueryReason.INTERCEPTOR)*. Therefore, developers
 *     can explicitly know whether the data being saved exists
 *     and whether it will be inserted or update. As a result,
 *     it has more functionality but lower performance.
 *
 *  -  [DraftPreProcessor]
 *
 *     Jimmer does not check whether the data being
 *     saved exists and unconditionally calls it,
 *     setting default values for unloaded properties
 *     of the object being saved.
 *     Because it doesn't perform any checks, it has
 *     higher performance but less functionality.
 *
 * @param <E> Entity Type
 * @param <D> Draft Type
 *
 * @see DraftPreProcessor
 */
interface DraftInterceptor<E: Any, D : Draft> {

    /**
     * Adjust draft before save
     *
     * <p>
     *  Note, if the other function [beforeSaveAll] is overridden,
     *  this method may not be automatically called by Jimmer.
     *  It depends on the overriding logic of method [beforeSaveAll].
     * </p>
     *
     * @param draft The draft can be modified, `id` and `key` properties cannot be changed, otherwise, exception will be raised.
     * @param original The original object
     *
     *  * null for insert
     *  * non-null for update, with `id`, `key` and other properties
     * returned by [.dependencies]
     */
    fun beforeSave(draft: D, original: E?) {}

    /**
     * In general, developers should override method
     * [beforeSave] instead of the current method.
     *
     * <p>However, in some scenarios, users may execute
     * some additional queries to determine the
     * subsequent logic. In this case, this method
     * can be overridden to avoid the `N+1` query problem
     * to reach better performance.</p>
     */
    fun beforeSaveAll(items: Collection<Item<E, D>>) {
        for (item in items) {
            beforeSave(item.draft, item.original)
        }
    }

    /**
     * Specify which properties of original entity must be loaded
     *
     * **Note**
     *
     * -   The return value must be stable, It will only be called once, so an unstable return is meaningless
     * -   All elements must be properties which is mapped by database field directly
     *
     * @return The properties must be loaded, can return null.
     */
    fun dependencies(): Collection<TypedProp<E, *>>? {
        return emptyList()
    }

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
     * If multiple DraftInterceptors act on an id-only
     * object, and any `DraftInterceptor` intends to
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
     * If multiple DraftInterceptors act on an key-only
     * object, and any `DraftInterceptor` intends to
     * ignore the modification operation, the modification
     * operation will be ignored finally.
     *
     * The return value of this method must be stable,
     * and different calls must return the same return value.
     */
    fun ignoreKeyOnly(group: KeyMatcher.Group) = false

    data class Item<E: Any, D: Draft>(
        val draft: D,
        val original: E?,
        val state: State
    ) {
        data class State(
            val keyGroup: KeyMatcher.Group?,
            val isIdOnly: Boolean,
            val isKeyOnly: Boolean
        )
    }
}
