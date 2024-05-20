package org.babyfish.jimmer.sql

import org.babyfish.jimmer.Draft

/**
 * Before saving draft, give user a chance to modify it.
 *
 * <p>This interface is very similar to another interface
 * [DraftInterceptor],
 * the differences between the two are as follows:
 * <ul>
 * <li>This interface is more conducive to SQL optimization, but has weaker functions</li>
 * <li>[DraftInterceptor] has stronger features but is not carp SQL optimized</li>
 * </ul>
 * </p>
 *
 * @see DraftInterceptor
 */
interface DraftPreProcessor<D: Draft> {

    fun beforeSave(draft: D)
}