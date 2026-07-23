package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TableAliases {

    public static final TableAliases EMPTY = new TableAliases(Collections.emptyMap());

    private final Map<RealTable, Alias> aliasMap;

    private int sequence;

    @Nullable
    private RealTable primaryTable;

    @Nullable
    private List<RealTable> additionalTables;

    @Nullable
    private Map<TableAliasKey, Alias> aliasMapByKey;

    private TableAliases(Map<RealTable, Alias> aliasMap) {
        this.aliasMap = aliasMap;
    }

    public static TableAliases allocate(
            Collection<RealTable> rootTables,
            Map<RealTable, TableUsedState> tableStateMap
    ) {
        if (rootTables.isEmpty()) {
            return EMPTY;
        }
        Map<RealTable, Alias> aliasMap = new IdentityHashMap<>(
                Math.max(rootTables.size(), tableStateMap.size())
        );
        TableAliases aliases = new TableAliases(aliasMap);
        for (RealTable rootTable : rootTables) {
            allocate(rootTable, true, tableStateMap, aliases);
        }
        return aliases;
    }

    @Nullable
    Alias get(RealTable table) {
        Alias alias = aliasMap.get(table);
        if (alias != null || aliasMap.isEmpty()) {
            return alias;
        }
        Map<TableAliasKey, Alias> aliasMapByKey = this.aliasMapByKey;
        if (aliasMapByKey == null) {
            aliasMapByKey = createAliasMapByKey();
            this.aliasMapByKey = aliasMapByKey;
        }
        return aliasMapByKey.get(table.getAliasKey());
    }

    public int getAliasCount() {
        return sequence;
    }

    private static void allocate(
            RealTable table,
            boolean root,
            Map<RealTable, TableUsedState> tableStateMap,
            TableAliases aliases
    ) {
        if (aliases.aliasMap.containsKey(table)) {
            return;
        }
        if (!root && !isUsed(table, tableStateMap)) {
            for (RealTable childTable : table) {
                allocate(childTable, false, tableStateMap, aliases);
            }
            return;
        }
        TableLikeImplementor<?> owner = table.getTableLikeImplementor();
        if (owner instanceof BaseTableImplementor) {
            BaseTableImplementor baseTable = (BaseTableImplementor) owner;
            BaseTableImplementor recursive = baseTable.getRecursive();
            if (recursive != null) {
                RealTable recursiveTable = recursive.realTable(table.getKey().scope);
                allocate(recursiveTable, true, tableStateMap, aliases);
                Alias recursiveAlias = aliases.aliasMap.get(recursiveTable);
                if (recursiveAlias != null) {
                    aliases.put(table, recursiveAlias);
                }
                return;
            }
        }
        String middleAlias = aliases.allocateMiddleAlias(owner);
        Alias alias = new Alias(aliases.allocateTableAlias(), middleAlias);
        aliases.put(table, alias);
        for (RealTable childTable : table) {
            allocate(childTable, false, tableStateMap, aliases);
        }
    }

    private void put(RealTable table, Alias alias) {
        aliasMap.put(table, alias);
        if (primaryTable == null) {
            primaryTable = table;
            return;
        }
        List<RealTable> additionalTables = this.additionalTables;
        if (additionalTables == null) {
            additionalTables = this.additionalTables = new ArrayList<>();
        }
        additionalTables.add(table);
    }

    private Map<TableAliasKey, Alias> createAliasMapByKey() {
        Map<TableAliasKey, Alias> aliasMapByKey = new HashMap<>();
        RealTable primaryTable = this.primaryTable;
        if (primaryTable != null) {
            aliasMapByKey.put(primaryTable.getAliasKey(), aliasMap.get(primaryTable));
        }
        List<RealTable> additionalTables = this.additionalTables;
        if (additionalTables != null) {
            for (RealTable table : additionalTables) {
                aliasMapByKey.put(table.getAliasKey(), aliasMap.get(table));
            }
        }
        return aliasMapByKey;
    }

    private static boolean isUsed(
            RealTable table,
            Map<RealTable, TableUsedState> tableStateMap
    ) {
        TableUsedState state = tableStateMap.get(table);
        return state == TableUsedState.USED;
    }

    private String allocateMiddleAlias(TableLikeImplementor<?> owner) {
        ImmutableProp joinProp = owner instanceof TableImplementor<?> ?
                ((TableImplementor<?>) owner).getJoinProp() :
                null;
        return joinProp != null && joinProp.isMiddleTableDefinition() ?
                allocateTableAlias() :
                null;
    }

    private String allocateTableAlias() {
        return "tb_" + ++sequence + '_';
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
