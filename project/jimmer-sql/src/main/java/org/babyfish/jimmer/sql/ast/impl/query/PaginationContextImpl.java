package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.PaginationContext;

import java.util.ArrayList;
import java.util.List;

public class PaginationContextImpl implements PaginationContext {
    
    private int limit;
    
    private int offset;
    
    private String originSql;
    
    private List<Object> originVariables;

    private StringBuilder builder = new StringBuilder();

    private List<Object> variables = new ArrayList<>();

    private boolean originApplied = false;

    public PaginationContextImpl(
            int limit, 
            int offset, 
            String originSql, 
            List<Object> originVariables
    ) {
        this.limit = limit;
        this.offset = offset;
        this.originSql = originSql;
        this.originVariables = originVariables;
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
    public PaginationContext origin() {
        if (originApplied) {
            throw new IllegalStateException("origin() can only be called once");
        }
        builder.append(originSql);
        variables.addAll(originVariables);
        originApplied = true;
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
        variables.add(value);
        builder.append("?");
        return this;
    }

    public Tuple2<String, List<Object>> build() {
        if (!originApplied) {
            throw new IllegalStateException("origin() has not been called");
        }
        return new Tuple2<>(builder.toString(), variables);
    }
}
