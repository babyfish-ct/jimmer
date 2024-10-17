package org.babyfish.jimmer.sql.ast.mutation;

import java.util.Map;

public class SimpleSaveResult<E> extends AbstractMutationResult {

    private E originalEntity;

    private E modifiedEntity;

    public SimpleSaveResult(
            Map<AffectedTable, Integer> affectedRowCountMap,
            E originalEntity,
            E modifiedEntity
    ) {
        super(affectedRowCountMap);
        this.originalEntity = originalEntity;
        this.modifiedEntity = modifiedEntity;
    }

    public E getOriginalEntity() {
        return originalEntity;
    }

    public E getModifiedEntity() {
        return modifiedEntity;
    }

    /**
     * If it is true, that means the save object is changed,
     * such as,
     * <ul>
     *     <li>The id is assigned to generated value</li>
     *     <li>Version is increased</li>
     *     <li>The back reference of children of one-to-many association is set</li>
     * </ul>
     * otherwise, the {@link #getOriginalEntity()} and {@link #getModifiedEntity()}
     * should be same object.
     */
    public boolean isModified() {
        return originalEntity != modifiedEntity;
    }

    public boolean isRowAffected() {
        return !affectedRowCountMap.isEmpty();
    }

    @Override
    public int hashCode() {
        int hash = affectedRowCountMap.hashCode();
        hash = hash * 31 + System.identityHashCode(originalEntity);
        hash = hash * 31 + System.identityHashCode(modifiedEntity);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleSaveResult<?> that = (SimpleSaveResult<?>) o;
        return affectedRowCountMap.equals(that.affectedRowCountMap) &&
                originalEntity == that.originalEntity &&
                modifiedEntity == that.modifiedEntity;
    }

    @Override
    public String toString() {
        return "SimpleSaveResult{" +
                "totalAffectedRowCount=" + totalAffectedRowCount +
                ", affectedRowCountMap=" + affectedRowCountMap +
                ", originalEntity=" + originalEntity +
                ", modifiedEntity=" + modifiedEntity +
                '}';
    }
}
