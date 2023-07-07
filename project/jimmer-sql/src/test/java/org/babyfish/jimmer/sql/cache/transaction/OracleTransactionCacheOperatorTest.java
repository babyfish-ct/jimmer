package org.babyfish.jimmer.sql.cache.transaction;

import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;

import javax.sql.DataSource;

public class OracleTransactionCacheOperatorTest extends AbstractTransactionCacheOperatorTest {

    @Override
    protected void assume() {
        NativeDatabases.assumeOracleDatabase();
    }

    @Override
    protected DataSource dataSource() {
        return NativeDatabases.ORACLE_DATA_SOURCE;
    }

    @Override
    protected Dialect dialect() {
        return new OracleDialect();
    }
}
