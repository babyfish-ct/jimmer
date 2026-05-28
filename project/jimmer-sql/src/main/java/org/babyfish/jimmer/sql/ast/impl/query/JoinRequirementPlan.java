package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

final class JoinRequirementPlan {

    private final Map<TableImplementor<?>, JoinType> map = new IdentityHashMap<>();

    void require(TableImplementor<?> table, JoinType joinType) {
        JoinType oldJoinType = map.get(table);
        if (oldJoinType == null || oldJoinType == joinType) {
            map.put(table, joinType);
        } else {
            map.put(table, JoinType.INNER);
        }
    }

    @Nullable
    JoinType get(TableImplementor<?> table) {
        return map.get(table);
    }
}
