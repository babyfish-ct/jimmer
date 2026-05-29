package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TableUsageCollector extends TableUsageVisitor {

    private final List<RealTable> rootTables = new ArrayList<>();

    private final Map<RealTable, TableUsedState> tableStateMap = new IdentityHashMap<>();

    public TableUsageCollector(AstContext ctx) {
        super(ctx);
    }

    public TableUsageCollector(AstContext ctx, QueryAnalysis queryAnalysis) {
        super(ctx, queryAnalysis);
    }

    public TableUsages toTableUsages() {
        return new TableUsages(rootTables, tableStateMap);
    }

    @Override
    protected void addRootTable(RealTable table) {
        rootTables.add(table);
    }

    @Override
    protected void useTableId(RealTable table) {
        tableStateMap.putIfAbsent(table, TableUsedState.ID_ONLY);
    }

    @Override
    protected void useTable(RealTable table) {
        tableStateMap.put(table, TableUsedState.USED);
    }

    protected final TableUsedState getTableUsedState(RealTable table) {
        TableUsedState state = tableStateMap.get(table);
        return state != null ? state : TableUsedState.NONE;
    }
}
