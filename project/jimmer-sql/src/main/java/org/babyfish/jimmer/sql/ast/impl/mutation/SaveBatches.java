package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class SaveBatches {

    private SaveBatches() {
    }

    static Batch<DraftSpi> of(
            Shape shape,
            EntityCollection<DraftSpi> entities,
            SaveMode mode,
            SaveMode originalMode
    ) {
        return new Batch<DraftSpi>() {

            @Override
            public Shape shape() {
                return shape;
            }

            @Override
            public EntityCollection<DraftSpi> entities() {
                return entities;
            }

            @Override
            public SaveMode mode() {
                return mode;
            }

            @Override
            public SaveMode originalMode() {
                return originalMode;
            }
        };
    }

    static Iterable<Batch<DraftSpi>> associationBatches(
            PreHandler preHandler,
            @Nullable Set<DraftSpi> acceptedDrafts
    ) {
        return acceptedDrafts != null ?
                filter(preHandler.associationBatches(), acceptedDrafts) :
                preHandler.associationBatches();
    }

    static Iterable<Batch<DraftSpi>> selfBatches(
            PreHandler preHandler,
            @Nullable Set<DraftSpi> acceptedDrafts
    ) {
        return acceptedDrafts != null ?
                filter(preHandler.batches(), acceptedDrafts) :
                preHandler.batches();
    }

    static Iterable<Batch<DraftSpi>> selfBatches(
            List<SaveOperation> operations,
            List<SaveSelfResult> selfResults
    ) {
        List<Batch<DraftSpi>> batches = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            SaveSelfResult selfResult = selfResults.get(i);
            for (Batch<DraftSpi> batch : selfBatches(operation.preHandler, selfResult.acceptedDrafts)) {
                batches.add(batch);
            }
        }
        return batches;
    }

    static Set<DraftSpi> acceptedDrafts(
            List<SaveOperation> operations,
            List<SaveSelfResult> selfResults
    ) {
        Set<DraftSpi> acceptedDrafts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            Set<DraftSpi> operationAcceptedDrafts = selfResults.get(i).acceptedDrafts;
            if (operationAcceptedDrafts != null) {
                acceptedDrafts.addAll(operationAcceptedDrafts);
            } else {
                acceptedDrafts.addAll(operation.drafts);
            }
        }
        return acceptedDrafts;
    }

    static List<DraftSpi> drafts(List<DraftSpi> drafts, @Nullable Set<DraftSpi> acceptedDrafts) {
        if (acceptedDrafts == null) {
            return drafts;
        }
        List<DraftSpi> filteredDrafts = new ArrayList<>(acceptedDrafts.size());
        for (DraftSpi draft : drafts) {
            if (acceptedDrafts.contains(draft)) {
                filteredDrafts.add(draft);
            }
        }
        return filteredDrafts;
    }

    static int[] acceptedRowCounts(int size) {
        int[] rowCounts = new int[size];
        Arrays.fill(rowCounts, 1);
        return rowCounts;
    }

    static void collectAcceptedDrafts(
            Set<DraftSpi> output,
            EntityCollection<DraftSpi> entities,
            int[] rowCounts
    ) {
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                output.add(item.getEntity());
            }
        }
    }

    private static Iterable<Batch<DraftSpi>> filter(
            Iterable<Batch<DraftSpi>> batches,
            Set<DraftSpi> acceptedDrafts
    ) {
        List<Batch<DraftSpi>> filteredBatches = new ArrayList<>();
        for (Batch<DraftSpi> batch : batches) {
            EntityList<DraftSpi> entities = new EntityList<>();
            for (DraftSpi draft : batch.entities()) {
                if (acceptedDrafts.contains(draft)) {
                    entities.add(draft);
                }
            }
            if (!entities.isEmpty()) {
                filteredBatches.add(new FilteredBatch(batch, entities));
            }
        }
        return filteredBatches;
    }

    private static class FilteredBatch implements Batch<DraftSpi> {

        private final Batch<DraftSpi> base;

        private final EntityCollection<DraftSpi> entities;

        private FilteredBatch(Batch<DraftSpi> base, EntityCollection<DraftSpi> entities) {
            this.base = base;
            this.entities = entities;
        }

        @Override
        public Shape shape() {
            return base.shape();
        }

        @Override
        public EntityCollection<DraftSpi> entities() {
            return entities;
        }

        @Override
        public SaveMode mode() {
            return base.mode();
        }

        @Override
        public SaveMode originalMode() {
            return base.originalMode();
        }
    }
}
