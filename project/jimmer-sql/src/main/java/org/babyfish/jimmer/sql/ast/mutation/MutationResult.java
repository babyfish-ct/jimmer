package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;

import java.util.Map;

public interface MutationResult {

    int getTotalAffectedRowCount();

    Map<AffectedTable, Integer> getAffectedRowCountMap();

    int getAffectedRowCount(AffectedTable affectTable);

    int getAffectedRowCount(Class<?> entityType);

    int getAffectedRowCount(TypedProp.Association<?, ?> associationProp);

    int getAffectedRowCount(ImmutableProp prop);
}