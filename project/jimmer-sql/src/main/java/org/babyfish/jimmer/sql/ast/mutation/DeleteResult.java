package org.babyfish.jimmer.sql.ast.mutation;

import java.util.Map;

public class DeleteResult extends AbstractMutationResult {

    public DeleteResult(Map<String, Integer> affectedRowCountMap) {
        super(affectedRowCountMap);
    }

    @Override
    public int hashCode() {
        return affectedRowCountMap.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeleteResult that = (DeleteResult) o;
        return affectedRowCountMap.equals(that.affectedRowCountMap);
    }

    @Override
    public String toString() {
        return "DeleteResult{" +
                "totalAffectedRowCount=" + totalAffectedRowCount +
                ", tableAffectedRowCountMap=" + affectedRowCountMap +
                '}';
    }
}

