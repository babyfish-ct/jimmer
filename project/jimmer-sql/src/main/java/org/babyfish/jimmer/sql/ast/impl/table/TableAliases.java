package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
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
import java.util.Objects;

public final class TableAliases {

    public static final TableAliases EMPTY = new TableAliases(Collections.emptyMap(), Collections.emptyMap());

    private final Map<RealTable, Alias> aliasMap;

    private final Map<AliasKey, Alias> aliasMapByKey;

    private TableAliases(Map<RealTable, Alias> aliasMap, Map<AliasKey, Alias> aliasMapByKey) {
        this.aliasMap = Collections.unmodifiableMap(aliasMap);
        this.aliasMapByKey = Collections.unmodifiableMap(aliasMapByKey);
    }

    public static TableAliases allocate(
            Collection<RealTable> rootTables,
            Map<RealTable, TableUsedState> tableStateMap,
            TableAliasAllocator allocator
    ) {
        if (rootTables.isEmpty()) {
            return EMPTY;
        }
        Map<RealTable, Alias> aliasMap = new IdentityHashMap<>();
        Map<AliasKey, Alias> aliasMapByKey = new HashMap<>();
        for (RealTable rootTable : rootTables) {
            allocate(rootTable, true, tableStateMap, aliasMap, aliasMapByKey, allocator);
        }
        return new TableAliases(aliasMap, aliasMapByKey);
    }

    @Nullable
    Alias get(RealTable table) {
        Alias alias = aliasMap.get(table);
        return alias != null ? alias : aliasMapByKey.get(AliasKey.of(table));
    }

    private static void allocate(
            RealTable table,
            boolean root,
            Map<RealTable, TableUsedState> tableStateMap,
            Map<RealTable, Alias> aliasMap,
            Map<AliasKey, Alias> aliasMapByKey,
            TableAliasAllocator allocator
    ) {
        if (aliasMap.containsKey(table)) {
            return;
        }
        if (!root && !isUsed(table, tableStateMap)) {
            for (RealTable childTable : table) {
                allocate(childTable, false, tableStateMap, aliasMap, aliasMapByKey, allocator);
            }
            return;
        }
        TableLikeImplementor<?> owner = table.getTableLikeImplementor();
        if (owner instanceof BaseTableImplementor) {
            BaseTableImplementor baseTable = (BaseTableImplementor) owner;
            BaseTableImplementor recursive = baseTable.getRecursive();
            if (recursive != null) {
                RealTable recursiveTable = recursive.realTable(table.getKey().scope);
                allocate(recursiveTable, true, tableStateMap, aliasMap, aliasMapByKey, allocator);
                Alias recursiveAlias = aliasMap.get(recursiveTable);
                if (recursiveAlias != null) {
                    aliasMap.put(table, recursiveAlias);
                    aliasMapByKey.put(AliasKey.of(table), recursiveAlias);
                }
                return;
            }
        }
        String middleAlias = allocateMiddleAlias(owner, allocator);
        Alias alias = new Alias(allocator.allocateTableAlias(owner), middleAlias);
        aliasMap.put(table, alias);
        aliasMapByKey.put(AliasKey.of(table), alias);
        for (RealTable childTable : table) {
            allocate(childTable, false, tableStateMap, aliasMap, aliasMapByKey, allocator);
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
            TableAliasAllocator allocator
    ) {
        ImmutableProp joinProp = owner instanceof TableImplementor<?> ?
                ((TableImplementor<?>) owner).getJoinProp() :
                null;
        return joinProp != null && joinProp.isMiddleTableDefinition() ?
                allocator.allocateTableAlias(owner) :
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

    private static final class AliasKey {

        private final AbstractMutableStatementImpl statement;

        private final List<RealTable.Key> path;

        private AliasKey(AbstractMutableStatementImpl statement, List<RealTable.Key> path) {
            this.statement = statement;
            this.path = path;
        }

        static AliasKey of(RealTable table) {
            List<RealTable.Key> path = new ArrayList<>();
            for (RealTable t = table; t != null; t = t.getParent()) {
                path.add(0, t.getKey());
            }
            return new AliasKey(table.getTableLikeImplementor().getStatement(), path);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AliasKey)) {
                return false;
            }
            AliasKey other = (AliasKey) o;
            return statement == other.statement && path.equals(other.path);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(statement) + Objects.hash(path);
        }
    }
}
