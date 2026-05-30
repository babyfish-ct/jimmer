package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Set<BaseTableOwner> owners = new HashSet<>();
        owners.addAll(fullRowExports);
        owners.addAll(expressionExports);
        owners.addAll(tableReferenceUsageMap.keySet());
        return owners;
    }

    static final class Builder {

        private final Set<BaseTableOwner> fullRowExports = new HashSet<>();

        private final Set<BaseTableOwner> expressionExports = new HashSet<>();

        private final Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap = new LinkedHashMap<>();

        void requireFullRowExport(BaseTableOwner baseTableOwner) {
            fullRowExports.addAll(expandedOwners(baseTableOwner));
        }

        void requireExpressionExport(BaseTableOwner baseTableOwner) {
            expressionExports.addAll(expandedOwners(baseTableOwner));
        }

        void requireTableReference(RealTable table, ImmutableProp prop, boolean rawId) {
            BaseTableOwner baseTableOwner = table.getBaseTableOwner();
            if (baseTableOwner == null) {
                return;
            }
            TableReferenceUsage usage = new TableReferenceUsage(table, prop, rawId);
            for (BaseTableOwner owner : expandedOwners(baseTableOwner)) {
                tableReferenceUsageMap
                        .computeIfAbsent(owner, it -> new ArrayList<>())
                        .add(usage);
            }
        }

        BaseQueryExportUsages build() {
            if (fullRowExports.isEmpty() && expressionExports.isEmpty() && tableReferenceUsageMap.isEmpty()) {
                return EMPTY;
            }
            Map<BaseTableOwner, List<TableReferenceUsage>> tableReferenceUsageMap = new LinkedHashMap<>();
            for (Map.Entry<BaseTableOwner, List<TableReferenceUsage>> e : this.tableReferenceUsageMap.entrySet()) {
                tableReferenceUsageMap.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            return new BaseQueryExportUsages(
                    new HashSet<>(fullRowExports),
                    new HashSet<>(expressionExports),
                    tableReferenceUsageMap
            );
        }

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
    }

    static final class TableReferenceUsage {

        final RealTable table;

        final ImmutableProp prop;

        final boolean rawId;

        TableReferenceUsage(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
            this.table = table;
            this.prop = prop;
            this.rawId = rawId;
        }
    }
}
