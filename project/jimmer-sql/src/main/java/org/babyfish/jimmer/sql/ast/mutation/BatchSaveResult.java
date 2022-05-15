package org.babyfish.jimmer.sql.ast.mutation;

import java.util.*;

public class BatchSaveResult<E> extends AbstractMutationResult {

    private List<SimpleSaveResult<E>> simpleResults;

    public BatchSaveResult(
            List<SimpleSaveResult<E>> simpleResults
    ) {
        this(mergedAffectedRowCount(simpleResults), simpleResults);
    }

    public BatchSaveResult(
            Map<AffectedTable, Integer> affectedRowMap,
            List<SimpleSaveResult<E>> simpleResults
    ) {
        super(affectedRowMap);
        this.simpleResults = Collections.unmodifiableList(simpleResults);
    }

    public List<SimpleSaveResult<E>> getSimpleResults() {
        return simpleResults;
    }

    @Override
    public int hashCode() {
        return Objects.hash(affectedRowCountMap, simpleResults);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchSaveResult<?> that = (BatchSaveResult<?>) o;
        return affectedRowCountMap.equals(that.affectedRowCountMap) &&
                simpleResults.equals(that.simpleResults);
    }

    @Override
    public String toString() {
        return "BatchSaveResult{" +
                "totalAffectedRowCount=" + totalAffectedRowCount +
                ", affectedRowCountMap=" + affectedRowCountMap +
                ", simpleResults=" + simpleResults +
                '}';
    }

    private static <E> Map<AffectedTable, Integer> mergedAffectedRowCount(
            List<SimpleSaveResult<E>> simpleResults
    ) {
        if (simpleResults.isEmpty()) {
            return Collections.emptyMap();
        }
        if (simpleResults.size() == 1) {
            return simpleResults.get(0).getAffectedRowCountMap();
        }
        Map<AffectedTable, Integer> mergedMap = new HashMap<>();
        for (SimpleSaveResult<?> result : simpleResults) {
            for (Map.Entry<AffectedTable, Integer> e : result.getAffectedRowCountMap().entrySet()) {
                mergedMap.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        return mergedMap;
    }
}
