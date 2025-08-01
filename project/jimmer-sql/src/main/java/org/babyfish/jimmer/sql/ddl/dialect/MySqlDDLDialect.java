package org.babyfish.jimmer.sql.ddl.dialect;

import org.babyfish.jimmer.sql.ddl.DDLUtils;
import org.babyfish.jimmer.sql.ddl.DatabaseVersion;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;

import static java.sql.Types.*;

/**
 * @author honhimW
 */

public class MySqlDDLDialect extends DefaultDDLDialect {

    public MySqlDDLDialect() {
        this(null);
    }

    public MySqlDDLDialect(final DatabaseVersion version) {
        this(new MySqlDialect(), version);
    }

    public MySqlDDLDialect(final Dialect dialect, final DatabaseVersion version) {
        super(dialect, version);
    }

    @Override
    public char openQuote() {
        return '`';
    }

    @Override
    public char closeQuote() {
        return '`';
    }

    @Override
    public String getColumnComment(String comment) {
        return " comment '" + comment + "'";
    }

    @Override
    public String getTableComment(String comment) {
        return " comment='" + comment + "'";
    }

    @Override
    public boolean supportsCommentOn() {
        return false;
    }

    @Override
    public boolean supportsColumnCheck() {
        return isSameOrAfter(8);
    }

    @Override
    public String columnType(int jdbcType, Long length, Integer precision, Integer scale) {
        length = getLength(jdbcType, length);
        precision = getPrecision(jdbcType, precision);
        scale = getScale(jdbcType, scale);
        switch (jdbcType) {
            case BOOLEAN:
                // HHH-6935: Don't use "boolean" i.e. tinyint(1) due to JDBC ResultSetMetaData
                return "bit";
            case TIMESTAMP:
                return DDLUtils.replace("datetime($p)", null, precision, null);
            case TIMESTAMP_WITH_TIMEZONE:
                return DDLUtils.replace("timestamp($p)", null, precision, null);
            case NUMERIC:
                // it's just a synonym
                return columnType(DECIMAL, length, precision, scale);

            // on MySQL 8, the nchar/nvarchar types use a deprecated character set
            case NCHAR:
                return DDLUtils.replace("char($l) character set utf8", length, null, null);
            case NVARCHAR:
                return DDLUtils.replace("varchar($l) character set utf8", length, null, null);

            // the maximum long LOB length is 4_294_967_295, bigger than any Java string
            case BLOB:
                return "longblob";
            case NCLOB:
                return "longtext character set utf8";
            case CLOB:
                return "longtext";
            default:
                return super.columnType(jdbcType, length, precision, scale);
        }
    }

    @Override
    public int getFloatPrecision(int jdbcType) {
        return 23;
    }

    @Override
    public String getIdentityColumnString(int type) {
        return "not null auto_increment";
    }

    @Override
    public String getTableTypeString() {
        return "engine=InnoDB";
    }

    @Override
    public String getDropForeignKeyString() {
        return "drop foreign key";
    }

    @Override
    public boolean supportsIfExistsBeforeConstraintName() {
        return false;
    }

    @Override
    public boolean supportsIfExistsAfterDropSequence() {
        return false;
    }

}
