package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class SqlBuilder {

    private SqlClient sqlClient;

    private Set<Table<?>> usedTables;

    private SqlBuilder parent;

    private int childBuilderCount;

    private StringBuilder builder;

    private List<Object> variables;

    private boolean terminated;

    public SqlBuilder(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
        this.usedTables = new HashSet<>();
    }

    private SqlBuilder(SqlBuilder parent) {
        this.sqlClient = parent.sqlClient;
        this.usedTables = parent.usedTables;
        this.parent = parent;
        parent.childBuilderCount++;
    }

    public void useTable(Table<?> table) {
        if (table != null) {
            usedTables.add(table);
        }
    }

    public boolean isTableUsed(Table<?> table) {
        return usedTables.contains(table);
    }

    public SqlBuilder sql(String sql) {
        builder.append(sql);
        return this;
    }

    public SqlBuilder variable(Object value) {
        variables.add(Objects.requireNonNull(value, "value cannot be null"));
        return this;
    }

    public SqlBuilder nullVariables(Class<?> type) {
        return this;
    }

    public SqlBuilder createChildBuilder() {
        return new SqlBuilder(this);
    }

    public Tuple2<String, List<Object>> build() {
        return build(null);
    }

    public Tuple2<String, List<Object>> build(
            Function<Tuple2<String, List<Object>>, Tuple2<String, List<Object>>> transformer
    ) {
        validate();
        Tuple2<String, List<Object>> result = new Tuple2<>(builder.toString(), variables);
        if (transformer != null) {
            result = transformer.apply(result);
        }
        SqlBuilder p = this.parent;
        if (p != null) {
            p.builder.append(result._1());
            p.variables.addAll(result._2());
            while (p != null) {
                --p.childBuilderCount;
                p = p.parent;
            }
        }
        terminated = true;
        return result;
    }

    private void validate() {
        if (childBuilderCount != 0) {
            throw new IllegalStateException(
                    "Internal bug: Cannot change sqlbuilder because there are some child builders"
            );
        }
        if (terminated) {
            throw new IllegalStateException(
                    "Internal bug: Current build has been terminated"
            );
        }
    }
}
