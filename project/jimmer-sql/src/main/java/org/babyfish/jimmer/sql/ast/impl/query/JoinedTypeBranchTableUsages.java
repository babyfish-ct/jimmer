package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

final class JoinedTypeBranchTableUsages {

    static final JoinedTypeBranchTableUsages EMPTY =
            new JoinedTypeBranchTableUsages(Collections.emptyMap());

    private final Map<TableImplementor<?>, Set<ImmutableType>> requiredStageTypes;

    JoinedTypeBranchTableUsages(Map<TableImplementor<?>, Set<ImmutableType>> requiredStageTypes) {
        this.requiredStageTypes = requiredStageTypes;
    }

    boolean isRequired(TableImplementor<?> table, ImmutableType stageType) {
        return stageTypes(table).contains(stageType);
    }

    Set<ImmutableType> stageTypes(TableImplementor<?> table) {
        Set<ImmutableType> stageTypes = requiredStageTypes.get(table);
        return stageTypes != null ? stageTypes : Collections.emptySet();
    }
}
