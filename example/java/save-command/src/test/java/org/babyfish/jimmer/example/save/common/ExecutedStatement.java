package org.babyfish.jimmer.example.save.common;

import org.babyfish.jimmer.sql.runtime.DbNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ExecutedStatement {

    private final String sql;

    private final List<?> variables;

    public ExecutedStatement(String sql, Object ... variables) {
        this.sql = sql;
        List<Object> variableList = new ArrayList<>(variables.length);
        for (Object variable : variables) {
            variableList.add(variable instanceof DbNull ? null : variable);
        }
        this.variables = Collections.unmodifiableList(variableList);
    }

    public String getSql() {
        return sql;
    }

    public List<?> getVariables() {
        return variables;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, variables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutedStatement that = (ExecutedStatement) o;
        return sql.equals(that.sql) && variables.equals(that.variables);
    }

    @Override
    public String toString() {
        return "ExecutedStatement{" +
                "sql='" + sql + '\'' +
                ", variables=" + variables +
                '}';
    }
}
