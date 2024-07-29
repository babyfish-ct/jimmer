package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;

import java.util.Map;

class AffectedRows {

    private AffectedRows() {}

    static void add(Map<AffectedTable, Integer> map, ImmutableType type, int count) {
        if (count != 0) {
            map.merge(AffectedTable.of(type), count, Integer::sum);
        }
    }

    static void add(Map<AffectedTable, Integer> map, ImmutableProp prop, int count) {
        if (count != 0) {
            map.merge(AffectedTable.of(prop), count, Integer::sum);
        }
    }
}
