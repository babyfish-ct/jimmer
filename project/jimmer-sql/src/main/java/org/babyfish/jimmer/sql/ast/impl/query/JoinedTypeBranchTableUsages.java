package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class JoinedTypeBranchTableUsages {

    static final JoinedTypeBranchTableUsages EMPTY = new JoinedTypeBranchTableUsages(Collections.emptySet());

    private final Set<TableImplementor<?>> requiredTables;

    JoinedTypeBranchTableUsages(Set<TableImplementor<?>> requiredTables) {
        if (requiredTables.isEmpty()) {
            this.requiredTables = Collections.emptySet();
        } else {
            this.requiredTables = Collections.newSetFromMap(new IdentityHashMap<>(requiredTables.size()));
            this.requiredTables.addAll(requiredTables);
        }
    }

    boolean isRequired(TableImplementor<?> table) {
        return requiredTables.contains(table);
    }
}
