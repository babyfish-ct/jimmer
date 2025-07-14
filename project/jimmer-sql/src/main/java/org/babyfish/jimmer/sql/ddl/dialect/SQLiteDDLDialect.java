package org.babyfish.jimmer.sql.ddl.dialect;

import org.babyfish.jimmer.sql.ddl.DatabaseVersion;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;

import static java.sql.Types.*;

/**
 * @author honhimW
 */

public class SQLiteDDLDialect extends DefaultDDLDialect {

    public SQLiteDDLDialect() {
        this(null);
    }

    public SQLiteDDLDialect(final DatabaseVersion version) {
        super(new SQLiteDialect(), version);
    }

    @Override
    public String columnType(int jdbcType, Long length, Integer precision, Integer scale) {
        length = getLength(jdbcType, length);
        precision = getPrecision(jdbcType, precision);
        scale = getScale(jdbcType, scale);
        switch (jdbcType) {
            case DECIMAL:
                return isSameOrAfter(3) ? super.columnType(jdbcType, length, precision, scale) : columnType(NUMERIC, length, precision, scale);
            case CHAR:
                return isSameOrAfter(3) ? super.columnType(jdbcType, length, precision, scale) : "char";
            case NCHAR:
                return isSameOrAfter(3) ? super.columnType(jdbcType, length, precision, scale) : "nchar";
            // No precision support
            case FLOAT:
                return "float";
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                return "timestamp";
            case TIME_WITH_TIMEZONE:
                return "time";
            case BINARY:
            case VARBINARY:
                return "blob";
            default:
                return super.columnType(jdbcType, length, precision, scale);
        }
    }

    @Override
    public boolean hasDataTypeInIdentityColumn() {
        return false;
    }

    @Override
    public String getIdentityColumnString(int type) {
        return "integer";
    }

    @Override
    public boolean supportsIfExistsBeforeConstraintName() {
        return false;
    }

    @Override
    public boolean supportsIfExistsAfterDropSequence() {
        return false;
    }

    @Override
    public boolean supportsCommentOn() {
        return false;
    }

    @Override
    public boolean hasAlterTable() {
        return false;
    }

    @Override
    public boolean supportsCreateTableWithForeignKey() {
        return true;
    }
}
