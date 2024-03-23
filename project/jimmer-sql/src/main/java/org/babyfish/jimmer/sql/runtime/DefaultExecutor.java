package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.*;

public class DefaultExecutor implements Executor {

    public static final DefaultExecutor INSTANCE = new DefaultExecutor();

    DefaultExecutor() {}

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        String sql = args.sql;
        List<Object> variables = args.variables;
        JSqlClientImplementor sqlClient = args.sqlClient;
        try (PreparedStatement stmt = args.statementFactory != null ?
                args.statementFactory.preparedStatement(args.con, sql) :
                args.con.prepareStatement(sql)
        ) {
            ParameterIndex parameterIndex = new ParameterIndex();
            for (Object variable : variables) {
                if (variable instanceof DbLiteral) {
                    DbLiteral literal = (DbLiteral) variable;
                    literal.setParameter(
                            stmt,
                            parameterIndex,
                            sqlClient
                    );
                } else if (variable instanceof TypedList<?>) {
                    TypedList<?> typedList = (TypedList<?>) variable;
                    stmt.setArray(
                            parameterIndex.get(),
                            args.con.createArrayOf(typedList.getSqlElementType(), typedList.toArray())
                    );
                } else {
                    stmt.setObject(parameterIndex.get(), variable);
                }
            }
            return args.block.apply(stmt);
        } catch (Exception ex) {
            throw new ExecutionException(
                    "Cannot execute SQL statement: " +
                            sql +
                            ", variables: " +
                            variables,
                    ex
            );
        }
    }
}
