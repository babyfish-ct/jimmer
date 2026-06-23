package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasScope;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

import java.util.*;

public final class TableUsages {

    static final TableUsages EMPTY = new TableUsages(Collections.emptyList(), Collections.emptyMap());

    private final List<RealTable> rootTables;

    private final Map<RealTable, TableUsedState> tableStateMap;

    TableUsages(List<RealTable> rootTables, Map<RealTable, TableUsedState> tableStateMap) {
        this.rootTables = Collections.unmodifiableList(new ArrayList<>(rootTables));
        this.tableStateMap = new IdentityHashMap<>(tableStateMap);
    }

    public void applyUsedStatesTo(AstContext astContext) {
        for (Map.Entry<RealTable, TableUsedState> e : tableStateMap.entrySet()) {
            if (e.getValue() == TableUsedState.USED) {
                astContext.useTable(e.getKey());
            } else if (e.getValue() == TableUsedState.ID_ONLY) {
                astContext.useTableId(e.getKey());
            }
        }
    }

    public void allocateAndBindAliases(AstContext astContext) {
        allocateAndBindAliases(astContext, null);
    }

    TableAliases allocateAndBindAliases(AstContext astContext, CteTableDependencies cteTableDependencies) {
        TableAliasScope aliasScope = astContext.beginTableAliasScope();
        TableAliases aliases = TableAliases.allocate(
                cteTableDependencies != null ? cteTableDependencies.aliasRootTables() : rootTables,
                tableStateMap,
                aliasScope
        );
        for (RealTable rootTable : rootTables) {
            aliasScope.applyAliases(rootTable, aliases);
        }
        return aliases;
    }

    List<RealTable> getRootTables() {
        return rootTables;
    }
}
