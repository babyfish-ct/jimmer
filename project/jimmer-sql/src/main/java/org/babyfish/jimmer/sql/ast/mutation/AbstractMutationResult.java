package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;

import java.util.Collections;
import java.util.Map;

public class AbstractMutationResult implements MutationResult {

    protected int totalAffectedRowCount;

    protected Map<AffectedTable, Integer> affectedRowCountMap;

    public AbstractMutationResult(Map<AffectedTable, Integer> affectedRowCountMap) {
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
    public Map<AffectedTable, Integer> getAffectedRowCountMap() {
        return affectedRowCountMap;
    }

    @Override
    public int getAffectedRowCount(AffectedTable affectTable) {
        Integer affectedRowCount = affectedRowCountMap.get(affectTable);
        return affectedRowCount != null ? affectedRowCount : 0;
    }

    @Override
    public int getAffectedRowCount(Class<?> entityType) {
        return getAffectedRowCount(AffectedTable.of(entityType));
    }

    @Override
    public int getAffectedRowCount(TypedProp.Association<?, ?> associationProp) {
        return getAffectedRowCount(AffectedTable.of(associationProp.unwrap()));
    }

    @Override
    public int getAffectedRowCount(ImmutableProp prop) {
        return getAffectedRowCount(AffectedTable.of(prop));
    }
}
