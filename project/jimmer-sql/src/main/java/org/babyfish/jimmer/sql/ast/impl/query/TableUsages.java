package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TableUsages {

    static final TableUsages EMPTY =
            new TableUsages(Collections.emptyList(), Collections.emptyMap());

    private final List<RealTable> rootTables;

    private final Map<RealTable, TableUsedState> tableStateMap;

    TableUsages(List<RealTable> rootTables, Map<RealTable, TableUsedState> tableStateMap) {
        this.rootTables = Collections.unmodifiableList(new ArrayList<>(rootTables));
        this.tableStateMap = new IdentityHashMap<>(tableStateMap);
    }

    public void applyTo(AstContext astContext) {
        for (Map.Entry<RealTable, TableUsedState> e : tableStateMap.entrySet()) {
            if (e.getValue() == TableUsedState.USED) {
                astContext.useTable(e.getKey());
            } else if (e.getValue() == TableUsedState.ID_ONLY) {
                astContext.useTableId(e.getKey());
            }
        }
    }

    public void allocateAliases() {
        for (RealTable rootTable : rootTables) {
            rootTable.allocateAliases();
        }
    }
}
