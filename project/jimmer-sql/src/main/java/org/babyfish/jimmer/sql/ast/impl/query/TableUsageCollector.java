package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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
    public void visitTableFetcherField(RealTable table, Field field) {
        BaseTableOwner baseTableOwner = table.getBaseTableOwner();
        if (baseTableOwner == null || field.getChildFetcher() == null) {
            super.visitTableFetcherField(table, field);
            return;
        }
        ImmutableProp prop = field.getProp();
        Storage storage = prop.getStorage(getAstContext().getSqlClient().getMetadataStrategy());
        if (!(storage instanceof EmbeddedColumns)) {
            super.visitTableFetcherField(table, field);
            return;
        }
        use(table);
        baseQueryExportUsagesBuilder.requireTableColumns(
                table,
                prop,
                embeddedColumnNames((EmbeddedColumns) storage, field.getChildFetcher())
        );
        use(realTable(getAstContext().resolveBaseTable(baseTableOwner.getBaseTable())));
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

    private static Collection<String> embeddedColumnNames(EmbeddedColumns columns, Fetcher<?> fetcher) {
        List<String> columnNames = new ArrayList<>();
        collectEmbeddedColumnNames(columns, fetcher, "", columnNames);
        return columnNames;
    }

    private static void collectEmbeddedColumnNames(
            EmbeddedColumns columns,
            Fetcher<?> fetcher,
            String path,
            List<String> columnNames
    ) {
        for (Field field : fetcher.getFieldMap().values()) {
            String propName = field.getProp().getName();
            String childPath = path.isEmpty() ? propName : path + '.' + propName;
            Fetcher<?> childFetcher = field.getChildFetcher();
            if (childFetcher == null) {
                EmbeddedColumns.Partial partial = columns.partial(childPath);
                int size = partial.size();
                for (int i = 0; i < size; i++) {
                    columnNames.add(partial.name(i));
                }
            } else {
                collectEmbeddedColumnNames(columns, childFetcher, childPath, columnNames);
            }
        }
    }
}
