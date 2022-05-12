package org.babyfish.jimmer.sql.ast.mutation;

import java.util.Collections;
import java.util.Map;

public class AbstractMutationResult implements MutationResult {

    protected int totalAffectedRowCount;

    protected Map<String, Integer> affectedRowCountMap;

    public AbstractMutationResult(Map<String, Integer> affectedRowCountMap) {
        this.affectedRowCountMap = Collections.unmodifiableMap(affectedRowCountMap);
        int totalAffectedRowCount = 0;
        for (Integer affectedRowCount : affectedRowCountMap.values()) {
            totalAffectedRowCount += affectedRowCount;
        }
        this.totalAffectedRowCount = totalAffectedRowCount;
    }

    @Override
    public int getTotalAffectedRowCount() {
        return totalAffectedRowCount;
    }

    @Override
    public Map<String, Integer> getAffectedRowCountMap() {
        return affectedRowCountMap;
    }

    @Override
    public int getAffectedRowCount(String tableName) {
        Integer affectedRowCount = affectedRowCountMap.get(tableName);
        return affectedRowCount != null ? affectedRowCount : 0;
    }
}
