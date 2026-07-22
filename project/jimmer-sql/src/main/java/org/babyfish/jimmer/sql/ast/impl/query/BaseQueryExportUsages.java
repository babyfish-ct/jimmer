package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class BaseQueryExportUsages {

    static final BaseQueryExportUsages EMPTY =
            new BaseQueryExportUsages(Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());

    private final Set<BaseTableOwner> fullRowExports;

    private final Set<BaseTableOwner> expressionExports;

    private final Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap;

    BaseQueryExportUsages(
            Set<BaseTableOwner> fullRowExports,
            Set<BaseTableOwner> expressionExports,
            Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap
    ) {
        this.fullRowExports = fullRowExports;
        this.expressionExports = expressionExports;
        this.tableReferenceUsageMap = tableReferenceUsageMap;
    }

    boolean isFullRowExportRequired(BaseTableOwner baseTableOwner) {
        return fullRowExports.contains(baseTableOwner);
    }

    boolean isExpressionExportRequired(BaseTableOwner baseTableOwner) {
        return expressionExports.contains(baseTableOwner);
    }

    List<TableReferenceUsage> tableReferenceUsages(BaseTableOwner baseTableOwner) {
        List<TableReferenceUsage> usages = tableReferenceUsageMap.get(baseTableOwner);
        return usages != null ? usages : Collections.emptyList();
    }

    Set<BaseTableOwner> owners() {
        if (fullRowExports.isEmpty() && expressionExports.isEmpty() && tableReferenceUsageMap.isEmpty()) {
            return Collections.emptySet();
        }
        Set<BaseTableOwner> owners = new LinkedHashSet<>();
        owners.addAll(fullRowExports);
        owners.addAll(expressionExports);
        owners.addAll(tableReferenceUsageMap.keySet());
        return owners;
    }

    static final class Builder {

        @Nullable
        private State state;

        private static List<BaseTableOwner> expandedOwners(BaseTableOwner baseTableOwner) {
            BaseTableSymbol baseTable = baseTableOwner.getBaseTable();
            MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
            if (mergedBy == null) {
                return Collections.singletonList(baseTableOwner);
            }
            List<BaseTableOwner> owners = new ArrayList<>();
            owners.add(baseTableOwner);
            boolean cte = baseTable.isCte();
            for (ConfigurableBaseQueryImpl<?> itemQuery : mergedBy.getExpandedQueries()) {
                owners.add(
                        new BaseTableOwner(
                                mergedBy.itemBaseTable(itemQuery, cte),
                                baseTableOwner.getIndex()
                        )
                );
            }
            return owners;
        }

        void requireFullRowExport(BaseTableOwner baseTableOwner) {
            state().fullRowExports.addAll(expandedOwners(baseTableOwner));
        }

        void requireExpressionExport(BaseTableOwner baseTableOwner) {
            state().expressionExports.addAll(expandedOwners(baseTableOwner));
        }

        void requireTableReference(BaseTableOwner baseTableOwner, RealTable table, ImmutableProp prop, boolean rawId) {
            requireTableReference(baseTableOwner, table, prop, rawId, null);
        }

        void requireTableColumns(
                BaseTableOwner baseTableOwner,
                RealTable table,
                ImmutableProp prop,
                Collection<String> columnNames
        ) {
            requireTableReference(baseTableOwner, table, prop, false, columnNames);
        }

        private void requireTableReference(
                BaseTableOwner baseTableOwner,
                RealTable table,
                ImmutableProp prop,
                boolean rawId,
                @Nullable Collection<String> columnNames
        ) {
            TableReferenceUsage usage = new TableReferenceUsage(table, prop, rawId, columnNames);
            Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap = state().tableReferenceUsageMap;
            for (BaseTableOwner owner : expandedOwners(baseTableOwner)) {
                tableReferenceUsageMap
                        .computeIfAbsent(owner, it -> new ArrayList<>())
                        .add(usage);
            }
        }

        BaseQueryExportUsages build() {
            State state = this.state;
            if (state == null) {
                return EMPTY;
            }
            Set<BaseTableOwner> fullRowExports = state.fullRowExports;
            Set<BaseTableOwner> expressionExports = state.expressionExports;
            Map<BaseTableOwner, List<TableReferenceUsage>> sourceTableReferenceUsageMap =
                    state.tableReferenceUsageMap;
            Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap = new LinkedHashMap<>();
            for (Map.Entry<BaseTableOwner, List<TableReferenceUsage>> e : sourceTableReferenceUsageMap.entrySet()) {
                tableReferenceUsageMap.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            return new BaseQueryExportUsages(
                    new LinkedHashSet<>(fullRowExports),
                    new LinkedHashSet<>(expressionExports),
                    tableReferenceUsageMap
            );
        }

        private State state() {
            State state = this.state;
            if (state == null) {
                state = this.state = new State();
            }
            return state;
        }

        private static class State {

            final Set<BaseTableOwner> fullRowExports = new LinkedHashSet<>();

            final Set<BaseTableOwner> expressionExports = new LinkedHashSet<>();

            final Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap = new LinkedHashMap<>();
        }
    }

    static final class TableReferenceUsage {

        final RealTable table;

        final ImmutableProp prop;

        final boolean rawId;

        @Nullable
        final List<String> columnNames;

        TableReferenceUsage(
                RealTable table,
                @Nullable ImmutableProp prop,
                boolean rawId,
                @Nullable Collection<String> columnNames
        ) {
            this.table = table;
            this.prop = prop;
            this.rawId = rawId;
            this.columnNames = columnNames != null ? new ArrayList<>(columnNames) : null;
        }
    }
}
