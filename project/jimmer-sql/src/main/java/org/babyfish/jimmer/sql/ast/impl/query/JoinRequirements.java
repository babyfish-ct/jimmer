package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

final class JoinRequirements {

    @Nullable
    private Map<TableImplementor<?>, JoinType> map;

    void require(TableImplementor<?> table, JoinType joinType) {
        Map<TableImplementor<?>, JoinType> map = this.map;
        if (map == null) {
            map = this.map = new IdentityHashMap<>();
        }
        JoinType oldJoinType = map.get(table);
        if (oldJoinType == null || oldJoinType == joinType) {
            map.put(table, joinType);
        } else {
            map.put(table, JoinType.INNER);
        }
    }

    @Nullable
    JoinType get(TableImplementor<?> table) {
        Map<TableImplementor<?>, JoinType> map = this.map;
        return map != null ? map.get(table) : null;
    }
}
