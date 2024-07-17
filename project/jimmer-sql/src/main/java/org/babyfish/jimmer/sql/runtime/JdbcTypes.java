package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.dialect.Dialect;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.time.*;
import java.util.HashMap;
import java.util.Map;

public class JdbcTypes {

    private static final Map<Class<?>, Integer> SQL_TYPE_MAP;

    public static int toJdbcType(Class<?> type, Dialect dialect) {
        int jdbcType = dialect.resolveJdbcType(type);
        if (jdbcType == Types.OTHER) {
            Integer standardJdbcType = SQL_TYPE_MAP.get(type);
            if (standardJdbcType != null) {
                return standardJdbcType;
            }
        }
        return jdbcType;
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
