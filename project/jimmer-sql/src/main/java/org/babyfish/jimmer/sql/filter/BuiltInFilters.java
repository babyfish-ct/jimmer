package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.table.Props;

public interface BuiltInFilters {

    /**
     * Get the builtin `not deleted` filter by jimmer type
     *
     * <p>Note: The logical deleted property decorated by {@link org.babyfish.jimmer.sql.LogicalDeleted}
     * must be declared in the type directly, returns null if this property is defined in any supertype </p>
     *
     * @param immutableType Jimmer Type
     * @return Filter or null.
     */
    Filter<Props> getDeclaredNotDeletedFilter(ImmutableType immutableType);

    /**
     * Get the builtin `not deleted` filter by java type
     *
     * <p>Note: The logical deleted property decorated by {@link org.babyfish.jimmer.sql.LogicalDeleted}
     * must be declared in the type directly, returns null if this property is defined in any supertype </p>
     *
     * @param type Java Type
     * @return Filter or null.
     */
    Filter<Props> getDeclaredNotDeletedFilter(Class<?> type);

    /**
     * Get the builtin `already deleted` filter by jimmer type
     *
     * <p>Note: The logical deleted property decorated by {@link org.babyfish.jimmer.sql.LogicalDeleted}
     * must be declared in the type directly, returns null if this property is defined in any supertype </p>
     *
     * @param immutableType Jimmer Type
     * @return Filter or null.
     */
    Filter<Props> getDeclaredAlreadyDeletedFilter(ImmutableType immutableType);

    /**
     * Get the builtin `already deleted` filter by java type
     *
     * <p>Note: The logical deleted property decorated by {@link org.babyfish.jimmer.sql.LogicalDeleted}
     * must be declared in the type directly, returns null if this property is defined in any supertype </p>
     *
     * @param type Java Type
     * @return Filter or null.
     */
    Filter<Props> getDeclaredAlreadyDeletedFilter(Class<?> type);
}
