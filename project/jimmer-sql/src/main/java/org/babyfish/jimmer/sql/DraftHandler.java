package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface DraftHandler<D extends Draft, E> {

    /**
     * Adjust draft before save
     * @param draft The draft can be modified, `id` and `key` properties cannot be changed, otherwise, exception will be raised.
     * @param original The original object
     *                 <ul>
     *                 <li>null for insert</li>
     *                 <li>non-null for update, with `id`, `key` and other properties
     *                 returned by {@link #dependencies()}</li>
     *                 </ul>
     */
    void beforeSave(@NotNull D draft, @Nullable E original);

    /**
     * Specify which properties of original entity must be loaded
     *
     * <p>Note</p>
     * <ul>
     *     <li>The return value must be stable, It will only be called once, so an unstable return is meaningless</li>
     *     <li>All elements must be properties which is mapped by database field directly</li>
     * </ul>
     *
     * @return The properties must be loaded, can return null.
     */
    default Collection<ImmutableProp> dependencies() {
        return Collections.emptyList();
    }
}
