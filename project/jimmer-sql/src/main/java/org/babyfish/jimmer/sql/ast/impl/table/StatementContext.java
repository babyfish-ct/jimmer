package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

public class StatementContext {

    private final ExecutionPurpose purpose;

    private final FilterLevel filterLevel;

    private final boolean rootUserFiltersIgnored;

    public StatementContext(ExecutionPurpose purpose) {
        this(purpose, FilterLevel.DEFAULT);
    }

    public StatementContext(ExecutionPurpose purpose, FilterLevel filterLevel) {
        this(purpose, filterLevel, false);
    }

    public StatementContext(
            ExecutionPurpose purpose,
            FilterLevel filterLevel,
            boolean rootUserFiltersIgnored
    ) {
        this.purpose = purpose;
        this.filterLevel = filterLevel;
        this.rootUserFiltersIgnored = rootUserFiltersIgnored;
    }

    public ExecutionPurpose getPurpose() {
        return purpose;
    }

    public FilterLevel getFilterLevel() {
        return filterLevel;
    }

    public boolean isRootUserFiltersIgnored() {
        return rootUserFiltersIgnored;
    }

    @Override
    public String toString() {
        return "StatementContext{" +
                "purpose=" + purpose +
                ", filterLevel=" + filterLevel +
                ", rootUserFiltersIgnored=" + rootUserFiltersIgnored +
                '}';
    }
}
