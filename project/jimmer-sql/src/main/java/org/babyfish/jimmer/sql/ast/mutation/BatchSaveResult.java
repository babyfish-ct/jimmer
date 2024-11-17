package org.babyfish.jimmer.sql.ast.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BatchSaveResult<E> extends AbstractMutationResult {

    private List<Item<E>> items;

    public BatchSaveResult(
            Map<AffectedTable, Integer> affectedRowMap,
            List<Item<E>> items
    ) {
        super(affectedRowMap);
        this.items = Collections.unmodifiableList(items);
    }

    public List<Item<E>> getItems() {
        return items;
    }

    @Override
    public int hashCode() {
        return Objects.hash(affectedRowCountMap, items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchSaveResult<?> that = (BatchSaveResult<?>) o;
        return affectedRowCountMap.equals(that.affectedRowCountMap) &&
                items.equals(that.items);
    }

    @Override
    public String toString() {
        return "BatchSaveResult{" +
                "totalAffectedRowCount=" + totalAffectedRowCount +
                ", affectedRowCountMap=" + affectedRowCountMap +
                ", simpleResults=" + items +
                '}';
    }

    public static class Item<E> implements MutationResultItem<E> {

        private final E originalEntity;

        private final E modifiedEntity;

        public Item(E originalEntity, E modifiedEntity) {
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
        public String toString() {
            return "Item{" +
                    "originalEntity=" + originalEntity +
                    ", modifiedEntity=" + modifiedEntity +
                    '}';
        }
    }
}
