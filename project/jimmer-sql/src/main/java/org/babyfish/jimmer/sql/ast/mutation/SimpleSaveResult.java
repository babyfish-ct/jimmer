package org.babyfish.jimmer.sql.ast.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class SimpleSaveResult<E> extends AbstractMutationResult implements MutationResultItem<E> {

    final E originalEntity;

    final E modifiedEntity;

    public SimpleSaveResult(
            Map<AffectedTable, Integer> affectedRowCountMap,
            E originalEntity,
            E modifiedEntity
    ) {
        super(affectedRowCountMap);
        this.originalEntity = originalEntity;
        this.modifiedEntity = modifiedEntity;
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

    public <V extends org.babyfish.jimmer.View<E>> View<E, V> toView(
            Function<E, V> converter
    ) {
        return new View<>(
                affectedRowCountMap,
                originalEntity,
                modifiedEntity,
                converter.apply(modifiedEntity)
        );
    }

    public static class View<E, V extends org.babyfish.jimmer.View<E>> extends SimpleSaveResult<E> {

        private final V modifiedView;

        View(
                Map<AffectedTable, Integer> affectedRowCountMap,
                E originalEntity,
                E modifiedEntity,
                V modifiedView
        ) {
            super(affectedRowCountMap, originalEntity, modifiedEntity);
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
