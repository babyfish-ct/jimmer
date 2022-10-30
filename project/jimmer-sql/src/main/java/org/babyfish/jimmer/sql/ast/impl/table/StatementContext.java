package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

public class StatementContext {

    private final ExecutionPurpose purpose;

    private final boolean filterIgnored;

    private int tableAliasSequence;

    public StatementContext(ExecutionPurpose purpose, boolean filterIgnored) {
        this.purpose = purpose;
        this.filterIgnored = filterIgnored;
    }

    public ExecutionPurpose getPurpose() {
        return purpose;
    }

    public String allocateTableAlias() {
        return "tb_" + ++tableAliasSequence + '_';
    }

    public boolean isFilterIgnored() {
        return filterIgnored;
    }
}
