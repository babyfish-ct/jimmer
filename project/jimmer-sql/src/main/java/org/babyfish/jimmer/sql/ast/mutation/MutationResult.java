package org.babyfish.jimmer.sql.ast.mutation;

import java.util.Map;

public interface MutationResult {

    int getTotalAffectedRowCount();

    Map<String, Integer> getAffectedRowCountMap();

    int getAffectedRowCount(String tableName);
}