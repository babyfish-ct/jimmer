package org.babyfish.jimmer.sql.ast.impl.table;

import java.util.HashSet;
import java.util.Set;

public final class TableAliasScope implements TableAliasAllocator {

    private int sequence;

    private Set<String> aliases;

    @Override
    public String allocateTableAlias(TableLikeImplementor<?> owner) {
        Set<String> aliases = this.aliases;
        if (aliases == null) {
            this.aliases = aliases = new HashSet<>();
        }
        String alias;
        do {
            alias = "tb_" + ++sequence + '_';
        } while (!aliases.add(alias));
        return alias;
    }

    @Override
    public void reserveTableAlias(String alias) {
        if (alias == null) {
            return;
        }
        Set<String> aliases = this.aliases;
        if (aliases == null) {
            this.aliases = aliases = new HashSet<>();
        }
        aliases.add(alias);
    }
}
