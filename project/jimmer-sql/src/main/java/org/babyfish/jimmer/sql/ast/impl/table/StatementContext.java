package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

public class StatementContext {

    private final ExecutionPurpose purpose;

    private final FilterLevel filterLevel;

    public StatementContext(ExecutionPurpose purpose) {
        this.purpose = purpose;
        this.filterLevel = FilterLevel.DEFAULT;
    }

    public StatementContext(ExecutionPurpose purpose, FilterLevel filterLevel) {
        this.purpose = purpose;
        this.filterLevel = filterLevel;
    }

    public ExecutionPurpose getPurpose() {
        return purpose;
    }

    public FilterLevel getFilterLevel() {
        return filterLevel;
    }

    @Override
    public String toString() {
        return "StatementContext{" +
                "purpose=" + purpose +
                ", filterLevel=" + filterLevel +
                '}';
    }
}
