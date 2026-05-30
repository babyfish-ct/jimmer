package org.babyfish.jimmer.sql.ast.impl.table;

import java.util.HashSet;
import java.util.Set;

public final class TableAliasScope implements TableAliasAllocator {

    private int sequence;

    private final Set<String> aliases = new HashSet<>();

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
}
