package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.dialect.Dialect;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DefaultExecutor implements Executor {

    public static final DefaultExecutor INSTANCE = new DefaultExecutor();

    DefaultExecutor() {}

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        String sql = args.sql;
        List<Object> variables = args.variables;
        Dialect dialect = args.sqlClient.getDialect();
        try (PreparedStatement stmt = args.statementFactory != null ?
                args.statementFactory.preparedStatement(args.con, sql) :
                args.con.prepareStatement(sql)
        ) {
            int size = variables.size();
            for (int index = 0; index < size; index++) {
                Object variable = variables.get(index);
                if (variable instanceof DbNull) {
                    stmt.setNull(
                            index + 1,
                            toJdbcType(((DbNull)variable).getType(), dialect)
                    );
                } else {
                    stmt.setObject(index + 1, variable);
                }
            }
            return args.block.apply(stmt);
        } catch (SQLException ex) {
            throw new ExecutionException(
                    "Cannot execute SQL statement: " +
                            sql +
                            ", variables: " +
                            variables,
                    ex
            );
        }
    }

    private int toJdbcType(Class<?> type, Dialect dialect) {
        if (type == String.class) {
            return Types.VARCHAR;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Types.BOOLEAN;
        }
        if (type == char.class || type == Character.class) {
            return Types.CHAR;
        }
        if (type == byte.class || type == Byte.class) {
            return Types.TINYINT;
        }
        if (type == short.class || type == Short.class) {
            return Types.SMALLINT;
        }
        if (type == int.class || type == Integer.class) {
            return Types.INTEGER;
        }
        if (type == long.class || type == Long.class) {
            return Types.BIGINT;
        }
        if (type == float.class || type == Float.class) {
            return Types.FLOAT;
        }
        if (type == double.class || type == Double.class) {
            return Types.DOUBLE;
        }
        if (type == BigInteger.class) {
            return Types.BIGINT;
        }
        if (type == BigDecimal.class) {
            return Types.DECIMAL;
        }
        if (type == UUID.class) {
            return Types.VARCHAR;
        }
        if (type == Date.class || type == java.sql.Date.class) {
            return Types.DATE;
        }
        if (type == Timestamp.class) {
            return Types.TIMESTAMP;
        }
        if (type == LocalDate.class) {
            return Types.DATE;
        }
        if (type == LocalTime.class) {
            return Types.TIME;
        }
        if (type == LocalDateTime.class || type == ZonedDateTime.class) {
            return Types.TIMESTAMP;
        }
        if (type == byte[].class) {
            return Types.BINARY;
        }
        int jdbcType = dialect.resolveUnknownJdbcType(type);
        if (jdbcType != Types.OTHER) {
            return jdbcType;
        }
        throw new IllegalArgumentException(
                "Cannot convert the sql type '" +
                        type +
                        "' to java.sql.Types by the current dialect \"" +
                        dialect.getClass().getName() +
                        "\""
        );
    }
}
