package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.MutationPath;

import java.util.Map;

class AffectedRows {

    private AffectedRows() {}

    static void add(Map<AffectedTable, Integer> map, ImmutableType type, int count) {
        if (count != 0) {
            map.merge(AffectedTable.of(type), count, Integer::sum);
        }
    }

    static void add(Map<AffectedTable, Integer> map, MutationPath path, int count) {
        if (count != 0) {
            ImmutableProp prop = path.getProp();
            if (prop != null) {
                map.merge(AffectedTable.of(prop), count, Integer::sum);
            } else {
                ImmutableProp backProp = path.getBackProp();
                map.merge(AffectedTable.of(backProp), count, Integer::sum);
            }
        }
    }
}
