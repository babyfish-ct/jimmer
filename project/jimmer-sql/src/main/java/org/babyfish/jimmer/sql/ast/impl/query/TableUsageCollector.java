package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TableUsageCollector extends TableUsageVisitor {

    private final List<RealTable> rootTables = new ArrayList<>();

    private final Map<RealTable, TableUsedState> tableStateMap = new IdentityHashMap<>();

    private final BaseQueryExportUsages.Builder baseQueryExportUsagesBuilder =
            new BaseQueryExportUsages.Builder();

    public TableUsageCollector(AstContext ctx) {
        super(ctx);
    }

    public TableUsageCollector(AstContext ctx, QueryAnalysis queryAnalysis) {
        super(ctx, queryAnalysis);
    }

    public TableUsages toTableUsages() {
        return new TableUsages(rootTables, tableStateMap);
    }

    public BaseQueryExportUsages toBaseQueryExportUsages() {
        return baseQueryExportUsagesBuilder.build();
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

    @Override
    protected void visitBaseTableReference(
            BaseTableOwner baseTableOwner,
            RealTable table,
            @Nullable ImmutableProp prop,
            boolean rawId
    ) {
        if (prop == null) {
            baseQueryExportUsagesBuilder.requireFullRowExport(baseTableOwner);
        } else {
            baseQueryExportUsagesBuilder.requireTableReference(table, prop, rawId);
        }
    }

    @Override
    public void visitBaseTableExpression(BaseTableOwner baseTableOwner) {
        baseQueryExportUsagesBuilder.requireExpressionExport(baseTableOwner);
    }

    protected final TableUsedState getTableUsedState(RealTable table) {
        TableUsedState state = tableStateMap.get(table);
        return state != null ? state : TableUsedState.NONE;
    }
}
