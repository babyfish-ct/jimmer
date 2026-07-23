package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasScope;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TableUsages {

    static final TableUsages EMPTY = new TableUsages(Collections.emptyList(), Collections.emptyMap());

    private final List<RealTable> rootTables;

    private final Map<RealTable, TableUsedState> tableStateMap;

    TableUsages(List<RealTable> rootTables, Map<RealTable, TableUsedState> tableStateMap) {
        this.rootTables = rootTables;
        this.tableStateMap = tableStateMap;
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
        TableAliases aliases = allocateAliases(cteTableDependencies);
        bindAliases(astContext, aliases);
        return aliases;
    }

    TableAliases allocateAliases(CteTableDependencies cteTableDependencies) {
        List<RealTable> aliasRootTables = cteTableDependencies != null ?
                cteTableDependencies.aliasRootTables() :
                Collections.emptyList();
        return TableAliases.allocate(
                !aliasRootTables.isEmpty() ? aliasRootTables : rootTables,
                tableStateMap
        );
    }

    void bindAliases(AstContext astContext, TableAliases aliases) {
        TableAliasScope aliasScope = astContext.beginTableAliasScope(
                Math.max(rootTables.size(), tableStateMap.size()),
                aliases.getAliasCount()
        );
        for (RealTable rootTable : rootTables) {
            aliasScope.applyAliases(rootTable, aliases);
        }
    }

    List<RealTable> getRootTables() {
        return rootTables;
    }
}
