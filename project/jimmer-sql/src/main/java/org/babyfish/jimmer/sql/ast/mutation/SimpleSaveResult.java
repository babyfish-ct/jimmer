package org.babyfish.jimmer.sql.ast.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

/**
 * Result of saving one root entity and its supplied associations.
 *
 * <p>The entity snapshots and logical acceptance are defined by {@link MutationResultItem}.
 * Affected-row counts include associated entities and association tables, and are independent
 * of both {@link #isAccepted()} and {@link #isModified()}.</p>
 *
 * @param <E> The root entity type
 */
public class SimpleSaveResult<E> extends AbstractMutationResult implements MutationResultItem<E> {

    final E originalEntity;

    final E modifiedEntity;

    final boolean accepted;

    /** Creates an accepted result with the supplied row counts and entity snapshots. */
    public SimpleSaveResult(
            Map<AffectedTable, Integer> affectedRowCountMap,
            E originalEntity,
            E modifiedEntity
    ) {
        this(affectedRowCountMap, originalEntity, modifiedEntity, true);
    }

    /** Creates a result with an explicit logical acceptance state. */
    public SimpleSaveResult(
            Map<AffectedTable, Integer> affectedRowCountMap,
            E originalEntity,
            E modifiedEntity,
            boolean accepted
    ) {
        super(affectedRowCountMap);
        this.originalEntity = originalEntity;
        this.modifiedEntity = modifiedEntity;
        this.accepted = accepted;
    }

    @NotNull
    @Override
    public E getOriginalEntity() {
        return originalEntity;
    }

    @NotNull
    @Override
    public E getModifiedEntity() {
        return modifiedEntity;
    }

    @Override
    public boolean isAccepted() {
        return accepted;
    }

    @Override
    public int hashCode() {
        int hash = affectedRowCountMap.hashCode();
        hash = hash * 31 + System.identityHashCode(originalEntity);
        hash = hash * 31 + System.identityHashCode(modifiedEntity);
        hash = hash * 31 + Boolean.hashCode(accepted);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleSaveResult<?> that = (SimpleSaveResult<?>) o;
        return affectedRowCountMap.equals(that.affectedRowCountMap) &&
                originalEntity == that.originalEntity &&
                modifiedEntity == that.modifiedEntity &&
                accepted == that.accepted;
    }

    @Override
    public String toString() {
        return "SimpleSaveResult{" +
                "totalAffectedRowCount=" + totalAffectedRowCount +
                ", affectedRowCountMap=" + affectedRowCountMap +
                ", originalEntity=" + originalEntity +
                ", modifiedEntity=" + modifiedEntity +
                ", accepted=" + accepted +
                '}';
    }

    /** Converts the resulting entity to a view, preserving row counts, snapshots, and acceptance. */
    public <V extends org.babyfish.jimmer.View<E>> View<E, V> toView(
            Function<E, V> converter
    ) {
        return new View<>(
                affectedRowCountMap,
                originalEntity,
                modifiedEntity,
                accepted,
                converter.apply(modifiedEntity)
        );
    }

    /** A save result that also exposes the resulting entity as a view. */
    public static class View<E, V extends org.babyfish.jimmer.View<E>> extends SimpleSaveResult<E> {

        private final V modifiedView;

        View(
                Map<AffectedTable, Integer> affectedRowCountMap,
                E originalEntity,
                E modifiedEntity,
                boolean accepted,
                V modifiedView
        ) {
            super(affectedRowCountMap, originalEntity, modifiedEntity, accepted);
            this.modifiedView = modifiedView;
        }

        /** Returns the view converted from {@link #getModifiedEntity()}. */
        public V getModifiedView() {
            return modifiedView;
        }

        @Override
        public String toString() {
            return "SimpleSaveResult.View{" +
                    "totalAffectedRowCount=" + totalAffectedRowCount +
                    ", affectedRowCountMap=" + affectedRowCountMap +
                    ", originalEntity=" + originalEntity +
                    ", modifiedView=" + modifiedView +
                    '}';
        }
    }
}
