package org.babyfish.jimmer.sql.ast.mutation

interface MutationResultItem<E: Any> {

    val originalEntity: E

    val modifiedEntity: E

    /**
     * If it is true, that means the save object is changed,
     * such as,
     * -    The id is assigned to generated value
     * -    Version is increased
     * -    The back reference of children of one-to-many association is set
     * otherwise, the [originalEntity] and [modifiedEntity]
     * should be same object.
     */
    val isModified: Boolean
        get() = originalEntity !== modifiedEntity
}