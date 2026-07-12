package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TableUsageCollector extends TableUsageVisitor {

    private final List<RealTable> rootTables = new ArrayList<>();

    private final Map<RealTable, TableUsedState> tableStateMap = new IdentityHashMap<>();

    private final Set<TableImplementor<?>> joinedTypeBranchTableRequirements =
            Collections.newSetFromMap(new IdentityHashMap<>());

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

    public JoinedTypeBranchTableUsages toJoinedTypeBranchTableUsages() {
        return new JoinedTypeBranchTableUsages(joinedTypeBranchTableRequirements);
    }

    public boolean isJoinedTypeBranchTableRequired(TableImplementor<?> table) {
        return joinedTypeBranchTableRequirements.contains(table);
    }

    @Override
    public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
        collectJoinedTypeBranchTableRequirement(table, prop);
        collectJoinedTypeBranchJoinSourceRequirement(table);
        super.visitTableReference(table, prop, rawId);
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
        collectJoinedTypeBranchTableRequirement(table, field.getProp());
        BaseTableOwner baseTableOwner = table.getBaseTableOwner();
        if (baseTableOwner == null) {
            super.visitTableFetcherField(table, field);
            return;
        }
        ImmutableProp prop = field.getProp();
        Storage storage = prop.getStorage(getAstContext().getSqlClient().getMetadataStrategy());
        if (storage instanceof EmbeddedColumns && field.getChildFetcher() != null) {
            use(table);
            baseQueryExportUsagesBuilder.requireTableColumns(
                    canonicalTableOwner(baseTableOwner),
                    table,
                    prop,
                    embeddedColumnNames((EmbeddedColumns) storage, field.getChildFetcher())
            );
            useResolvedBaseTable(baseTableOwner);
            return;
        }
        baseQueryExportUsagesBuilder.requireTableReference(canonicalTableOwner(baseTableOwner), table, prop, false);
        useResolvedBaseTable(baseTableOwner);
    }

    @Override
    protected void visitBaseTableReference(
            BaseTableOwner baseTableOwner,
            RealTable table,
            @Nullable ImmutableProp prop,
            boolean rawId
    ) {
        if (prop == null) {
            baseQueryExportUsagesBuilder.requireFullRowExport(canonicalTableOwner(baseTableOwner));
        } else {
            baseQueryExportUsagesBuilder.requireTableReference(canonicalTableOwner(baseTableOwner), table, prop, rawId);
        }
    }

    @Override
    public void visitBaseTableExpression(BaseTableOwner baseTableOwner) {
        baseQueryExportUsagesBuilder.requireExpressionExport(canonicalTableOwner(baseTableOwner));
        useResolvedBaseTable(baseTableOwner);
    }

    protected final TableUsedState getTableUsedState(RealTable table) {
        TableUsedState state = tableStateMap.get(table);
        return state != null ? state : TableUsedState.NONE;
    }

    private void collectJoinedTypeBranchTableRequirement(RealTable table, @Nullable ImmutableProp prop) {
        TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
        if (implementor instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
            if (tableImplementor.isJoinedTypeBranchTableRequiredBy(prop)) {
                joinedTypeBranchTableRequirements.add(tableImplementor);
            }
        }
    }

    // A foreign key declared in a JOINED-inheritance subtype lives in the branch
    // table, so any join that reads it as its raw parent id must force the parent's
    // branch table to be rendered. Mirror the plain-column join conditions of
    // RealTableImpl.renderJoin: skip inverse, middle-table and SQL-template joins,
    // which never read a raw parent foreign key column.
    private void collectJoinedTypeBranchJoinSourceRequirement(RealTable table) {
        for (RealTable current = table; current != null; current = current.getParent()) {
            TableLikeImplementor<?> implementor = current.getTableLikeImplementor();
            if (!(implementor instanceof TableImplementor<?>)) {
                return;
            }
            TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
            ImmutableProp joinProp = tableImplementor.getJoinProp();
            if (joinProp == null ||
                    tableImplementor.isInverse() ||
                    joinProp.isMiddleTableDefinition() ||
                    joinProp.getSqlTemplate() instanceof JoinTemplate) {
                continue;
            }
            RealTable parent = current.getParent();
            if (parent != null) {
                collectJoinedTypeBranchTableRequirement(parent, joinProp);
            }
        }
    }

    private BaseTableOwner canonicalTableOwner(BaseTableOwner baseTableOwner) {
        return getAstContext().resolveBaseTableOwner(baseTableOwner);
    }

    private void useResolvedBaseTable(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = getAstContext().resolveBaseTable(baseTableOwner.getBaseTable());
        if (baseTable != null) {
            use(realTable(baseTable));
        }
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
