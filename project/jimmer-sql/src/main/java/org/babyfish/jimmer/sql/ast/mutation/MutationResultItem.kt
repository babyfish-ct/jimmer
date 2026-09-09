package org.babyfish.jimmer.sql.ast.mutation

/**
 * The input, resulting entity, and logical outcome of saving one entity.
 * Shared by single-save results and individual batch-save items in Java and Kotlin.
 */
interface MutationResultItem<E : Any> {

    /** The original immutable entity supplied to the save command; it is never mutated. */
    val originalEntity: E

    /**
     * The entity produced by saving, including resolved/generated ids, version changes,
     * and association changes. When a fetcher is supplied, accepted results are materialized
     * according to that fetcher. Without a fetcher, this is not necessarily a complete database snapshot.
     * A rejected save does not guarantee that requested fields have been fetched.
     */
    val modifiedEntity: E

    /**
     * Whether the root row was accepted by the save operation.
     * Insertions, updates, and accepted no-op or fake updates are accepted. An insert-if-absent
     * conflict, a false update condition, an incompatible discriminator, or an update target
     * excluded by the preliminary lookup is rejected.
     *
     * Acceptance is independent of [isModified] and affected-row counts: an accepted operation
     * can leave both the entity instance and the database row unchanged. In particular,
     * forbidding update assignments does not itself reject an existing row.
     */
    val isAccepted: Boolean
        get() = true

    /**
     * Whether [modifiedEntity] is a different instance from [originalEntity].
     * For example, resolving an id, increasing a version, setting child back references,
     * or materializing a fetcher can produce a new instance.
     * This is a reference comparison, not an indication that a database row was changed or accepted.
     */
    val isModified: Boolean
        get() = originalEntity !== modifiedEntity
}
