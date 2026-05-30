package org.babyfish.jimmer.sql.ast.impl.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TableAliasScope implements TableAliasAllocator {

    private int sequence;

    private final Set<String> aliases = new HashSet<>();

    private final Map<RealTable, AliasBinding> aliasBindings = new IdentityHashMap<>();

    private final Map<List<RealTable.Key>, AliasBinding> aliasBindingsByPath = new HashMap<>();

    private final Set<List<RealTable.Key>> ambiguousAliasPaths = new HashSet<>();

    @Override
    public String allocateTableAlias(TableLikeImplementor<?> owner) {
        String alias;
        do {
            alias = "tb_" + ++sequence + '_';
        } while (!aliases.add(alias));
        return alias;
    }

    @Override
    public void reserveTableAlias(String alias) {
        if (alias != null) {
            aliases.add(alias);
        }
    }

    AliasBinding bind(RealTable table, String value, String middleValue) {
        reserveTableAlias(value);
        reserveTableAlias(middleValue);
        AliasBinding binding = new AliasBinding(value, middleValue, null, middleValue != null);
        bind(table, binding);
        return binding;
    }

    AliasBinding bindLazy(
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
        return binding;
    }

    public String getAlias(RealTable table) {
        return binding(table).value();
    }

    public String getAliasIfBound(RealTable table) {
        AliasBinding binding = bindingOrNull(table);
        return binding != null ? binding.value() : null;
    }

    public String getMiddleTableAlias(RealTable table) {
        return binding(table).middleValue();
    }

    public String getMiddleTableAliasIfBound(RealTable table) {
        AliasBinding binding = bindingOrNull(table);
        return binding != null ? binding.middleValue() : null;
    }

    private void bind(RealTable table, AliasBinding binding) {
        aliasBindings.put(table, binding);
        List<RealTable.Key> path = path(table);
        if (ambiguousAliasPaths.contains(path)) {
            return;
        }
        AliasBinding existing = aliasBindingsByPath.putIfAbsent(path, binding);
        if (existing != null && existing != binding) {
            aliasBindingsByPath.remove(path);
            ambiguousAliasPaths.add(path);
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
        List<RealTable.Key> path = path(table);
        return ambiguousAliasPaths.contains(path) ? null : aliasBindingsByPath.get(path);
    }

    private static List<RealTable.Key> path(RealTable table) {
        List<RealTable.Key> path = new ArrayList<>();
        for (RealTable t = table; t != null; t = t.getParent()) {
            path.add(0, t.getKey());
        }
        return Collections.unmodifiableList(path);
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
