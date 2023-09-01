package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.dialect.Dialect;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.*;

public class DefaultExecutor implements Executor {

    public static final DefaultExecutor INSTANCE = new DefaultExecutor();

    private static final Map<Class<?>, Integer> SQL_TYPE_MAP;

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
        Integer sqlType = SQL_TYPE_MAP.get(type);
        if (sqlType != null) {
            return sqlType;
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

    static {
        Map<Class<?>, Integer> map = new HashMap<>();
        map.put(String.class, Types.VARCHAR);
        map.put(boolean.class, Types.TINYINT);
        map.put(Boolean.class, Types.TINYINT);
        map.put(char.class, Types.CHAR);
        map.put(Character.class, Types.CHAR);
        map.put(byte.class, Types.TINYINT);
        map.put(Byte.class, Types.TINYINT);
        map.put(short.class, Types.SMALLINT);
        map.put(Short.class, Types.SMALLINT);
        map.put(int.class, Types.INTEGER);
        map.put(Integer.class, Types.INTEGER);
        map.put(long.class, Types.BIGINT);
        map.put(Long.class, Types.BIGINT);
        map.put(float.class, Types.FLOAT);
        map.put(Float.class, Types.FLOAT);
        map.put(double.class, Types.DOUBLE);
        map.put(Double.class, Types.DOUBLE);
        map.put(BigInteger.class, Types.DECIMAL);
        map.put(BigDecimal.class, Types.DECIMAL);
        map.put(UUID.class, Types.VARCHAR);
        map.put(java.sql.Date.class, Types.DATE);
        map.put(java.sql.Time.class, Types.TIME);
        map.put(java.util.Date.class, Types.TIMESTAMP);
        map.put(LocalDate.class, Types.DATE);
        map.put(LocalTime.class, Types.TIME);
        map.put(LocalDateTime.class, Types.TIMESTAMP);
        map.put(OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
        map.put(ZonedDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
        map.put(byte[].class, Types.BINARY);
        SQL_TYPE_MAP = map;
    }
}
