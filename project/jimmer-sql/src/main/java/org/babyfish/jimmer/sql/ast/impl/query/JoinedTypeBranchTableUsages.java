package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;

import java.util.*;

final class JoinedTypeBranchTableUsages {

    static final JoinedTypeBranchTableUsages EMPTY =
            new JoinedTypeBranchTableUsages(Collections.emptyMap());

    private final Map<TableImplementor<?>, Set<ImmutableType>> requiredStageTypes;

    JoinedTypeBranchTableUsages(Map<TableImplementor<?>, Set<ImmutableType>> requiredStageTypes) {
        if (requiredStageTypes.isEmpty()) {
            this.requiredStageTypes = Collections.emptyMap();
        } else {
            Map<TableImplementor<?>, Set<ImmutableType>> map =
                    new IdentityHashMap<>(requiredStageTypes.size());
            for (Map.Entry<TableImplementor<?>, Set<ImmutableType>> e : requiredStageTypes.entrySet()) {
                map.put(e.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(e.getValue())));
            }
            this.requiredStageTypes = Collections.unmodifiableMap(map);
        }
    }

    boolean isRequired(TableImplementor<?> table, ImmutableType stageType) {
        return stageTypes(table).contains(stageType);
    }

    Set<ImmutableType> stageTypes(TableImplementor<?> table) {
        Set<ImmutableType> stageTypes = requiredStageTypes.get(table);
        return stageTypes != null ? stageTypes : Collections.emptySet();
    }
}
