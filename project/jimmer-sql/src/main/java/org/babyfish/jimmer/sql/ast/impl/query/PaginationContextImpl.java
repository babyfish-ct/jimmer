package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.dialect.PaginationContext;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;

import java.util.ArrayList;
import java.util.List;

public class PaginationContextImpl implements PaginationContext {

    private final SqlFormatter sqlFormatter;
    
    private final int limit;
    
    private final int offset;
    
    private final String originSql;
    
    private final List<Object> originVariables;

    private final List<Integer> originVariablePositions;

    private final boolean idOnly;

    private final StringBuilder builder = new StringBuilder();

    private final List<Object> variables = new ArrayList<>();

    private final List<Integer> variablePositions;

    private boolean originApplied = false;

    public PaginationContextImpl(
            SqlFormatter formatter,
            int limit,
            int offset,
            String originSql,
            List<Object> originVariables,
            List<Integer> originVariablePositions,
            boolean idOnly) {
        this.sqlFormatter = formatter;
        this.limit = limit;
        this.offset = offset;
        this.originSql = originSql;
        this.originVariables = originVariables;
        this.originVariablePositions = originVariablePositions;
        this.idOnly = idOnly;
        if (originVariablePositions != null) {
            variablePositions = new ArrayList<>();
        } else {
            variablePositions = null;
        }
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public boolean isIdOnly() {
        return idOnly;
    }

    @Override
    public PaginationContext origin() {
        if (originApplied) {
            throw new IllegalStateException("origin() can only be called once");
        }
        builder.append(originSql);
        variables.addAll(originVariables);
        if (variablePositions != null) {
            variablePositions.addAll(originVariablePositions);
        }
        originApplied = true;
        return this;
    }

    @Override
    public PaginationContext space() {
        if (sqlFormatter.isPretty()) {
            builder.append('\n');
        } else {
            builder.append(' ');
        }
        return this;
    }

    @Override
    public PaginationContext newLine() {
        if (sqlFormatter.isPretty()) {
            builder.append('\n');
        }
        return this;
    }

    @Override
    public PaginationContext sql(String sql) {
        builder.append(sql);
        return this;
    }

    @Override
    public PaginationContext variable(Object value) {
        if (!originApplied) {
            throw new IllegalStateException("Cannot add variables before the origin() is called");
        }
        builder.append("?");
        variables.add(value);
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
        return this;
    }

    public Tuple3<String, List<Object>, List<Integer>> build() {
        if (!originApplied) {
            throw new IllegalStateException("origin() has not been called");
        }
        return new Tuple3<>(builder.toString(), variables, variablePositions);
    }
}
