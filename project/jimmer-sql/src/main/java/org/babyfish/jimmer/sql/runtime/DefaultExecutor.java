package org.babyfish.jimmer.sql.runtime;

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
    public <R> R execute(
            Connection con,
            String sql,
            List<Object> variables,
            ExecutionPurpose purpose,
            StatementFactory statementFactory,
            SqlFunction<PreparedStatement, R> block
    ) {
        try (PreparedStatement stmt = statementFactory != null ?
                statementFactory.preparedStatement(con, sql) :
                con.prepareStatement(sql)
        ) {
            int size = variables.size();
            for (int index = 0; index < size; index++) {
                Object variable = variables.get(index);
                if (variable instanceof DbNull) {
                    stmt.setNull(
                            index + 1,
                            toJdbcType(((DbNull) variable).getType())
                    );
                } else {
                    stmt.setObject(index + 1, variable);
                }
            }
            return block.apply(stmt);
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

    private int toJdbcType(Class<?> type) {
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
        throw new IllegalArgumentException(
                "Cannot convert '" +
                        type +
                        "' to java.sql.Types"
        );
    }
}
