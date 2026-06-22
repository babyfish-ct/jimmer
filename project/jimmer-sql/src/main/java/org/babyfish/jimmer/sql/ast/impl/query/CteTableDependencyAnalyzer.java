package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;

import java.util.*;

final class CteTableDependencyAnalyzer {

    private CteTableDependencyAnalyzer() {
    }

    static CteTableDependencies analyze(
            List<AbstractMutableStatementImpl> statements,
            TableUsages tableUsages,
            AstContext astContext
    ) {
        if (statements.isEmpty()) {
            return CteTableDependencies.EMPTY;
        }
        Map<AbstractMutableStatementImpl, List<CteTableDeclaration>> declarationMap = new IdentityHashMap<>();
        for (AbstractMutableStatementImpl statement : statements) {
            TableLikeImplementor<?> tableLikeImplementor = statement.getTableLikeImplementor();
            if (!tableLikeImplementor.hasBaseTable()) {
                continue;
            }
            List<RealTable> tables = collectRenderTables(tableLikeImplementor.realTable(astContext));
            if (!tables.isEmpty()) {
                declarationMap.put(statement, toDeclarations(tables));
            }
        }
        List<RealTable> aliasRootTables = collectAliasRootTables(tableUsages.getRootTables(), astContext);
        return new CteTableDependencies(
                Collections.unmodifiableMap(declarationMap),
                Collections.unmodifiableList(aliasRootTables)
        );
    }

    private static List<RealTable> collectRenderTables(RealTable table) {
        State state = new State();
        state.collectCteTables(table);
        return state.result();
    }

    private static List<RealTable> collectAliasRootTables(List<RealTable> rootTables, AstContext astContext) {
        State state = new State();
        List<RealTable> orderedRootTables = new ArrayList<>(rootTables.size());
        for (RealTable rootTable : rootTables) {
            state.collectCteDependencies(rootTable, astContext);
            orderedRootTables.addAll(state.drain());
            if (!state.isVisited(rootTable)) {
                orderedRootTables.add(rootTable);
                state.markVisited(rootTable);
            }
        }
        return orderedRootTables;
    }

    private static List<CteTableDeclaration> toDeclarations(List<RealTable> tables) {
        List<CteTableDeclaration> declarations = new ArrayList<>(tables.size());
        for (RealTable table : tables) {
            BaseTableImplementor baseTable = (BaseTableImplementor) table.getTableLikeImplementor();
            declarations.add(new CteTableDeclaration(table, baseTable.toSymbol(), baseTable.isRecursiveCte()));
        }
        return Collections.unmodifiableList(declarations);
    }

    private static class State {

        private final List<RealTable> cteTables = new ArrayList<>();

        private final Set<RealTable> visiting = Collections.newSetFromMap(new IdentityHashMap<>());

        private final Set<RealTable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        List<RealTable> result() {
            return Collections.unmodifiableList(new ArrayList<>(cteTables));
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
