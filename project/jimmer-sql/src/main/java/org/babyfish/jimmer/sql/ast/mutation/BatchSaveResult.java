package org.babyfish.jimmer.sql.ast.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class BatchSaveResult<E> extends AbstractMutationResult {

    final List<Item<E>> items;

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
                ", items=" + items +
                '}';
    }

    public <V extends org.babyfish.jimmer.View<E>> BatchSaveResult.View<E, V> toView(
            Function<E, V> converter
    ) {
        List<View.ViewItem<E, V>> viewItems = new ArrayList<>(items.size());
        for (Item<E> item : items) {
            View.ViewItem<E, V> viewItem = new View.ViewItem<>(
                    item.originalEntity,
                    item.modifiedEntity,
                    converter.apply(item.modifiedEntity)
            );
            viewItems.add(viewItem);
        }
        return new View<>(affectedRowCountMap, viewItems);
    }

    public static class Item<E> implements MutationResultItem<E> {

        final E originalEntity;

        final E modifiedEntity;

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

    public static class View<E, V extends org.babyfish.jimmer.View<E>> extends BatchSaveResult<E> {

        @SuppressWarnings("unchecked")
        View(Map<AffectedTable, Integer> affectedRowMap, List<ViewItem<E, V>> items) {
            super(affectedRowMap, (List<BatchSaveResult.Item<E>>) (List<?>) items);
        }

        @SuppressWarnings("unchecked")
        public List<ViewItem<E, V>> getViewItems() {
            return (List<ViewItem<E, V>>)(List<?>) items;
        }

        @Override
        public String toString() {
            return "BatchSaveResult.View{" +
                    ", affectedRowCountMap=" + affectedRowCountMap +
                    ", totalAffectedRowCount=" + totalAffectedRowCount +
                    ", items=" + items +
                    '}';
        }

        public static class ViewItem<E, V> extends BatchSaveResult.Item<E> {

            private final V modifiedView;

            public ViewItem(E originalEntity, E modifiedEntity, V modifiedView) {
                super(originalEntity, modifiedEntity);
                this.modifiedView = modifiedView;
            }

            @NotNull
            public V getModifiedView() {
                return modifiedView;
            }

            @Override
            public String toString() {
                return "Item{" +
                        "originalEntity=" + originalEntity +
                        ", modifiedView=" + modifiedView +
                        '}';
            }
        }
    }
}
