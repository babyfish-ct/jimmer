package org.babyfish.jimmer.sql.kt.filter

import kotlin.reflect.KClass

interface KBuiltInFilters {

    /**
     * Get the builtin `not deleted` filter by jimmer type
     *
     *
     * Note: The logical deleted property decorated by [org.babyfish.jimmer.sql.LogicalDeleted]
     * must be declared in the type directly, returns null if this property is defined in any supertype
     *
     * @param type Kotlin type
     * @return Filter or null.
     */
    fun <T: Any> getDeclaredNotDeletedFilter(type: KClass<T>): KFilter<T>?

    /**
     * Get the builtin `already deleted` filter by java type
     *
     *
     * Note: The logical deleted property decorated by [org.babyfish.jimmer.sql.LogicalDeleted]
     * must be declared in the type directly, returns null if this property is defined in any supertype
     *
     * @param type Kotlin type
     * @return Filter or null.
     */
    fun <T: Any> getDeclaredAlreadyDeletedFilter(type: KClass<T>): KFilter<T>?
}