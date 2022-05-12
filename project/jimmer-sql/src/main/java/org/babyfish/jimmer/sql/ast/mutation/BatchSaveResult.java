package org.babyfish.jimmer.sql.ast.mutation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BatchSaveResult<E> extends AbstractMutationResult {

    private List<SimpleSaveResult<E>> simpleResults;

    public BatchSaveResult(
            Map<String, Integer> affectedRowCountMap,
            List<SimpleSaveResult<E>> simpleResults
    ) {
        super(affectedRowCountMap);
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
}
