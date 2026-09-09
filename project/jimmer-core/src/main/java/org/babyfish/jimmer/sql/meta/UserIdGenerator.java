package org.babyfish.jimmer.sql.meta;

/**
 * Generates an entity id in application code, without waiting for an insert result.
 *
 * <p>Save commands can use this generator with native upsert when the dialect and
 * other save options allow it. Matching by key is chosen before generation:
 * a generated id is used only by the insert arm and never replaces the id of a matched row.</p>
 */
public interface UserIdGenerator<T> extends IdGenerator {

    /**
     * Creates an id for a potential new row of the specified entity type.
     *
     * <p>This method can be called even when upsert ultimately matches an existing row
     * or rejects the insert. The generated value is then unused; calling this method
     * does not imply that a row will be inserted.</p>
     *
     * @param entityType The entity type being saved
     * @return A new, non-null id
     */
    T generate(Class<?> entityType);

    final class None implements UserIdGenerator<Object> {

        @Override
        public Object generate(Class<?> entityType) {
            throw new UnsupportedOperationException();
        }
    }
}
