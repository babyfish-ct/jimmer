package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TableAliasScope implements TableAliasAllocator {

    private int sequence;

    private final Map<RealTable, AliasBinding> aliasBindings;

    @Nullable
    private Map<TableAliasKey, AliasBinding> aliasBindingsByKey;

    @Nullable
    private Set<TableAliasKey> ambiguousAliasKeys;

    public TableAliasScope(int expectedTableCount) {
        this(expectedTableCount, 0);
    }

    public TableAliasScope(int expectedTableCount, int sequence) {
        this.sequence = sequence;
        aliasBindings = new IdentityHashMap<>(expectedTableCount);
    }

    @Override
    public String allocateTableAlias(TableLikeImplementor<?> owner) {
        return "tb_" + ++sequence + '_';
    }

    public void ensureAlias(RealTable table) {
        if (isIdentityBound(table)) {
            return;
        }
        RealTable parent = table.getParent();
        if (parent != null) {
            ensureAlias(parent);
        }
        if (!isIdentityBound(table)) {
            bindFallback(table);
        }
    }

    public void applyAliases(RealTable table, TableAliases aliases) {
        if (!isIdentityBound(table)) {
            TableAliases.Alias alias = aliases.get(table);
            if (alias != null) {
                bind(table, alias.value, alias.middleValue);
            }
        }
        for (RealTable childTable : table) {
            applyAliases(childTable, aliases);
        }
    }

    private void bind(RealTable table, String value, String middleValue) {
        AliasBinding binding = new AliasBinding(value, middleValue, null, middleValue != null);
        bind(table, binding);
    }

    private void bindLazy(
            RealTable table,
            TableLikeImplementor<?> owner,
            boolean middleTableDefinition,
            boolean middleFirst
    ) {
        String value = null;
        String middleValue = null;
        if (!middleTableDefinition || !middleFirst) {
            value = allocateTableAlias(owner);
        }
        if (middleTableDefinition && middleFirst) {
            middleValue = allocateTableAlias(owner);
        }
        AliasBinding binding = new AliasBinding(value, middleValue, owner, middleTableDefinition);
        bind(table, binding);
    }

    boolean isIdentityBound(RealTable table) {
        return aliasBindings.containsKey(table);
    }

    public void bindAlias(RealTable table, RealTable source) {
        AliasBinding binding = binding(source);
        bind(table, binding);
    }

    private void bindFallback(RealTable table) {
        TableLikeImplementor<?> owner = table.getTableLikeImplementor();
        if (owner instanceof BaseTableImplementor) {
            BaseTableImplementor baseTable = (BaseTableImplementor) owner;
            BaseTableImplementor recursive = baseTable.getRecursive();
            if (recursive != null) {
                RealTable recursiveTable = recursive.realTable(table.getKey().scope);
                ensureAlias(recursiveTable);
                bindAlias(table, recursiveTable);
                return;
            }
        }
        ImmutableProp joinProp = owner instanceof TableImplementor<?> ?
                ((TableImplementor<?>) owner).getJoinProp() :
                null;
        boolean middleTableDefinition = joinProp != null && joinProp.isMiddleTableDefinition();
        bindLazy(
                table,
                owner,
                middleTableDefinition,
                middleTableDefinition
        );
    }

    public String getAlias(RealTable table) {
        return binding(table).value();
    }

    @Nullable
    public String getAliasIfBound(RealTable table) {
        AliasBinding binding = bindingOrNull(table);
        return binding != null ? binding.value() : null;
    }

    @Nullable
    public String getMiddleTableAlias(RealTable table) {
        return binding(table).middleValue();
    }

    @Nullable
    public String getMiddleTableAliasIfBound(RealTable table) {
        AliasBinding binding = bindingOrNull(table);
        return binding != null ? binding.middleValue() : null;
    }

    private void bind(RealTable table, AliasBinding binding) {
        AliasBinding oldBinding = aliasBindings.put(table, binding);
        Map<TableAliasKey, AliasBinding> aliasBindingsByKey = this.aliasBindingsByKey;
        if (aliasBindingsByKey != null || (oldBinding != null && oldBinding != binding)) {
            updateStructuralBindings(table, binding, aliasBindingsByKey);
        }
    }

    private void updateStructuralBindings(
            RealTable table,
            AliasBinding binding,
            @Nullable Map<TableAliasKey, AliasBinding> aliasBindingsByKey
    ) {
        if (aliasBindingsByKey != null) {
            bindByKey(aliasBindingsByKey, table, binding);
        } else {
            ambiguousAliasKeys().add(table.getAliasKey());
        }
    }

    private void bindByKey(
            Map<TableAliasKey, AliasBinding> aliasBindingsByKey,
            RealTable table,
            AliasBinding binding
    ) {
        TableAliasKey key = table.getAliasKey();
        Set<TableAliasKey> ambiguousAliasKeys = this.ambiguousAliasKeys;
        if (ambiguousAliasKeys != null && ambiguousAliasKeys.contains(key)) {
            return;
        }
        AliasBinding existing = aliasBindingsByKey.putIfAbsent(key, binding);
        if (existing != null && existing != binding) {
            aliasBindingsByKey.remove(key);
            ambiguousAliasKeys().add(key);
        }
    }

    private AliasBinding binding(RealTable table) {
        return Objects.requireNonNull(
                bindingOrNull(table),
                "Table alias has not been bound for " + table
        );
    }

    private AliasBinding bindingOrNull(RealTable table) {
        AliasBinding binding = aliasBindings.get(table);
        if (binding != null) {
            return binding;
        }
        Map<TableAliasKey, AliasBinding> aliasBindingsByKey = this.aliasBindingsByKey;
        if (aliasBindingsByKey == null) {
            aliasBindingsByKey = new HashMap<>(aliasBindings.size());
            for (Map.Entry<RealTable, AliasBinding> e : aliasBindings.entrySet()) {
                bindByKey(aliasBindingsByKey, e.getKey(), e.getValue());
            }
            this.aliasBindingsByKey = aliasBindingsByKey;
        }
        TableAliasKey key = table.getAliasKey();
        Set<TableAliasKey> ambiguousAliasKeys = this.ambiguousAliasKeys;
        if (ambiguousAliasKeys != null && ambiguousAliasKeys.contains(key)) {
            return null;
        }
        binding = aliasBindingsByKey.get(key);
        if (binding != null) {
            aliasBindings.put(table, binding);
        }
        return binding;
    }

    private Set<TableAliasKey> ambiguousAliasKeys() {
        Set<TableAliasKey> ambiguousAliasKeys = this.ambiguousAliasKeys;
        if (ambiguousAliasKeys == null) {
            ambiguousAliasKeys = this.ambiguousAliasKeys = new HashSet<>();
        }
        return ambiguousAliasKeys;
    }

    final class AliasBinding {

        private String value;

        private String middleValue;

        private final TableLikeImplementor<?> owner;

        private final boolean middleTableDefinition;

        private AliasBinding(
                String value,
                String middleValue,
                TableLikeImplementor<?> owner,
                boolean middleTableDefinition
        ) {
            this.value = value;
            this.middleValue = middleValue;
            this.owner = owner;
            this.middleTableDefinition = middleTableDefinition;
        }

        String value() {
            String value = this.value;
            if (value == null) {
                value = this.value = allocateTableAlias(owner);
            }
            return value;
        }

        String middleValue() {
            if (!middleTableDefinition) {
                return null;
            }
            String middleValue = this.middleValue;
            if (middleValue == null) {
                middleValue = this.middleValue = allocateTableAlias(owner);
            }
            return middleValue;
        }
    }
}
