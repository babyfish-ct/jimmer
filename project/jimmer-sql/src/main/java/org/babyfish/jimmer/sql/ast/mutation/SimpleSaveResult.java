package org.babyfish.jimmer.sql.ast.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class SimpleSaveResult<E> extends AbstractMutationResult implements MutationResultItem<E> {

    final E originalEntity;

    final E modifiedEntity;

    final boolean accepted;

    public SimpleSaveResult(
            Map<AffectedTable, Integer> affectedRowCountMap,
            E originalEntity,
            E modifiedEntity
    ) {
        this(affectedRowCountMap, originalEntity, modifiedEntity, true);
    }

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
