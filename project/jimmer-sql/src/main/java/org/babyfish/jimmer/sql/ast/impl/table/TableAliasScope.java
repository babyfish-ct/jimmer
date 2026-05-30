package org.babyfish.jimmer.sql.ast.impl.table;

import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TableAliasScope implements TableAliasAllocator {

    private int sequence;

    private final Set<String> aliases = new HashSet<>();

    private final Map<RealTable, AliasBinding> aliasBindings = new IdentityHashMap<>();

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
        aliasBindings.put(table, binding);
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
        aliasBindings.put(table, binding);
        return binding;
    }

    public String getAlias(RealTable table) {
        return binding(table).value();
    }

    private AliasBinding binding(RealTable table) {
        return Objects.requireNonNull(
                aliasBindings.get(table),
                "Table alias has not been bound for " + table
        );
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
