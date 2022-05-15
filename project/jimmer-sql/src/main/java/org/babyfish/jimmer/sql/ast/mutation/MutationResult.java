package org.babyfish.jimmer.sql.ast.mutation;

import java.util.Map;

public interface MutationResult {

    int getTotalAffectedRowCount();

    Map<AffectedTable, Integer> getAffectedRowCountMap();

    int getAffectedRowCount(AffectedTable affectTable);
}