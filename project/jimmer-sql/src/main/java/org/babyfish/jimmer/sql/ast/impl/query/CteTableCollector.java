package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;

import java.util.*;

final class CteTableCollector {

    private CteTableCollector() {
    }

    static List<RealTable> collectRenderTables(TableLikeImplementor<?> tableLikeImplementor, AstContext astContext) {
        if (!tableLikeImplementor.hasBaseTable()) {
            return Collections.emptyList();
        }
        Collector collector = new Collector();
        collector.collectCteTables(tableLikeImplementor.realTable(astContext));
        return collector.result();
    }

    static List<RealTable> collectAliasRootTables(List<RealTable> rootTables, AstContext astContext) {
        Collector collector = new Collector();
        List<RealTable> orderedRootTables = new ArrayList<>(rootTables.size());
        for (RealTable rootTable : rootTables) {
            collector.collectCteDependencies(rootTable, astContext);
            orderedRootTables.addAll(collector.drain());
            if (!collector.isVisited(rootTable)) {
                orderedRootTables.add(rootTable);
                collector.markVisited(rootTable);
            }
        }
        return orderedRootTables;
    }

    private static class Collector {

        private final List<RealTable> cteTables = new ArrayList<>();

        private final Set<RealTable> visiting = Collections.newSetFromMap(new IdentityHashMap<>());

        private final Set<RealTable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        List<RealTable> result() {
            return cteTables;
        }

        List<RealTable> drain() {
            if (cteTables.isEmpty()) {
                return Collections.emptyList();
            }
            List<RealTable> tables = new ArrayList<>(cteTables);
            cteTables.clear();
            return tables;
        }

        boolean isVisited(RealTable table) {
            return visited.contains(table);
        }

        void markVisited(RealTable table) {
            visited.add(table);
        }

        void collectCteTables(RealTable table) {
            if (table.getTableLikeImplementor() instanceof BaseTableImplementor) {
                BaseTableImplementor baseTable = (BaseTableImplementor) table.getTableLikeImplementor();
                if (baseTable.getRecursive() != null) {
                    for (RealTable child : table) {
                        collectCteTables(child);
                    }
                    return;
                }
                if (baseTable.isCte()) {
                    collectCteTable(table);
                } else {
                    collectCteDependencies(
                            baseTable.getQuery(),
                            new AstContext(
                                    baseTable.getQuery().getSqlClient(),
                                    QueryRenderMode.WITHOUT_NESTED_BASE_TABLE_SORTING_AND_PAGING
                            )
                    );
                }
            }
            for (RealTable child : table) {
                collectCteTables(child);
            }
        }

        void collectCteDependencies(RealTable table, AstContext astContext) {
            if (!(table.getTableLikeImplementor() instanceof BaseTableImplementor)) {
                return;
            }
            BaseTableImplementor baseTable = (BaseTableImplementor) table.getTableLikeImplementor();
            if (baseTable.getRecursive() != null) {
                return;
            }
            if (!baseTable.isCte()) {
                collectCteDependencies(baseTable.getQuery(), astContext);
                return;
            }
            if (!visiting.add(table)) {
                return;
            }
            collectCteDependencies(baseTable.getQuery(), astContext);
            visiting.remove(table);
        }

        private void collectCteTable(RealTable table) {
            if (visited.contains(table) || !visiting.add(table)) {
                return;
            }
            BaseTableImplementor baseTable = (BaseTableImplementor) table.getTableLikeImplementor();
            if (baseTable.getRecursive() != null) {
                visiting.remove(table);
                return;
            }
            AstContext astContext = new AstContext(
                    baseTable.getQuery().getSqlClient(),
                    QueryRenderMode.WITHOUT_NESTED_BASE_TABLE_SORTING_AND_PAGING
            );
            collectCteDependencies(baseTable.getQuery(), astContext);
            visiting.remove(table);
            visited.add(table);
            cteTables.add(table);
        }

        private void collectCteDependencies(TypedBaseQueryImplementor<?> query, AstContext astContext) {
            if (query instanceof ConfigurableBaseQueryImpl<?>) {
                collectCteDependencies((ConfigurableBaseQueryImpl<?>) query, astContext);
                return;
            }
            List<ConfigurableBaseQueryImpl<?>> queries = new ArrayList<>();
            query.collectConfigurableQueries(queries);
            for (ConfigurableBaseQueryImpl<?> configurableQuery : queries) {
                collectCteDependencies(configurableQuery, astContext);
            }
        }

        private void collectCteDependencies(ConfigurableBaseQueryImpl<?> query, AstContext parentAstContext) {
            collectCteDependencies(query, parentAstContext, true);
        }

        private void collectCteDependencies(
                ConfigurableBaseQueryImpl<?> query,
                AstContext parentAstContext,
                boolean expandMergedBy
        ) {
            MergedBaseQueryImpl<?> mergedBy = query.getMergedBy();
            if (expandMergedBy && mergedBy != null) {
                for (ConfigurableBaseQueryImpl<?> itemQuery : mergedBy.getExpandedQueries()) {
                    collectCteDependencies(itemQuery, parentAstContext, false);
                }
                return;
            }
            AstContext astContext = new AstContext(
                    parentAstContext.getSqlClient(),
                    QueryRenderMode.WITHOUT_NESTED_BASE_TABLE_SORTING_AND_PAGING
            );
            astContext.pushStatement(query.getMutableQuery());
            try {
                TableLikeImplementor<?> tableLikeImplementor = query.getMutableQuery().getTableLikeImplementor();
                if (tableLikeImplementor.hasBaseTable()) {
                    collectCteTables(tableLikeImplementor.realTable(astContext));
                }
            } finally {
                astContext.popStatement();
            }
        }
    }
}
