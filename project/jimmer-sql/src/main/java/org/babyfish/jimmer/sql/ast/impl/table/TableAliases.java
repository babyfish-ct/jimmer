package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class TableAliases {

    public static final TableAliases EMPTY = new TableAliases(Collections.emptyMap());

    private final Map<RealTable, Alias> aliasMap;

    private TableAliases(Map<RealTable, Alias> aliasMap) {
        this.aliasMap = Collections.unmodifiableMap(aliasMap);
    }

    public static TableAliases allocate(
            Collection<RealTable> rootTables,
            Map<RealTable, TableUsedState> tableStateMap
    ) {
        if (rootTables.isEmpty()) {
            return EMPTY;
        }
        Map<RealTable, Alias> aliasMap = new IdentityHashMap<>();
        for (RealTable rootTable : rootTables) {
            allocate(rootTable, true, tableStateMap, aliasMap);
        }
        return new TableAliases(aliasMap);
    }

    @Nullable
    Alias get(RealTable table) {
        return aliasMap.get(table);
    }

    private static void allocate(
            RealTable table,
            boolean root,
            Map<RealTable, TableUsedState> tableStateMap,
            Map<RealTable, Alias> aliasMap
    ) {
        if (aliasMap.containsKey(table)) {
            return;
        }
        if (!root && !isUsed(table, tableStateMap)) {
            for (RealTable childTable : table) {
                allocate(childTable, false, tableStateMap, aliasMap);
            }
            return;
        }
        TableLikeImplementor<?> owner = table.getTableLikeImplementor();
        if (owner instanceof BaseTableImplementor) {
            BaseTableImplementor baseTable = (BaseTableImplementor) owner;
            if (baseTable.getRecursive() != null) {
                return;
            }
        }
        AbstractMutableStatementImpl statement = owner.getStatement();
        StatementContext stmtCtx = statement.getContext();
        String middleAlias = allocateMiddleAlias(owner, stmtCtx);
        aliasMap.put(table, new Alias(stmtCtx.allocateTableAlias(), middleAlias));
        for (RealTable childTable : table) {
            allocate(childTable, false, tableStateMap, aliasMap);
        }
    }

    private static boolean isUsed(
            RealTable table,
            Map<RealTable, TableUsedState> tableStateMap
    ) {
        TableUsedState state = tableStateMap.get(table);
        return state == TableUsedState.USED;
    }

    private static String allocateMiddleAlias(
            TableLikeImplementor<?> owner,
            StatementContext stmtCtx
    ) {
        ImmutableProp joinProp = owner instanceof TableImplementor<?> ?
                ((TableImplementor<?>) owner).getJoinProp() :
                null;
        return joinProp != null && joinProp.isMiddleTableDefinition() ?
                stmtCtx.allocateTableAlias() :
                null;
    }

    static final class Alias {

        final String value;

        final String middleValue;

        Alias(String value, String middleValue) {
            this.value = value;
            this.middleValue = middleValue;
        }
    }
}
