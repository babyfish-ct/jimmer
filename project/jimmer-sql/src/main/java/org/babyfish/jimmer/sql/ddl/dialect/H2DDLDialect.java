package org.babyfish.jimmer.sql.ddl.dialect;

import org.babyfish.jimmer.sql.EnumType;
import org.babyfish.jimmer.sql.ddl.DatabaseVersion;
import org.babyfish.jimmer.sql.dialect.H2Dialect;

import java.util.UUID;

import static java.sql.Types.*;

/**
 * @author honhimW
 */

public class H2DDLDialect extends DefaultDDLDialect {

    public H2DDLDialect() {
        this(null);
    }

    public H2DDLDialect(final DatabaseVersion version) {
        super(new H2Dialect(), version);
    }

    @Override
    public String getIdentityColumnString(int type) {
        return "not null auto_increment";
    }

    @Override
    public String columnType(int jdbcType, Long length, Integer precision, Integer scale) {
        length = getLength(jdbcType, length);
        precision = getPrecision(jdbcType, precision);
        scale = getScale(jdbcType, scale);
        switch (jdbcType) {
            case NCHAR:
                return columnType(CHAR, length, precision, scale);
            case NVARCHAR:
                return columnType(VARCHAR, length, precision, scale);
            default:
                return super.columnType(jdbcType, length, precision, scale);
        }
    }

    @Override
    public String resolveSqlType(Class<?> type, EnumType.Strategy strategy) {
        if (type == UUID.class) {
            return "uuid";
        }
        return super.resolveSqlType(type, strategy);
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return isSameOrAfter(1, 4);
    }

    @Override
    public boolean supportsIfExistsAfterTableName() {
        return !supportsIfExistsBeforeTableName();
    }

    @Override
    public String getCascadeConstraintsString() {
        return "cascade";
    }

    @Override
    public boolean supportsIfExistsAfterAlterTable() {
        return isSameOrAfter(1, 4);
    }
}
