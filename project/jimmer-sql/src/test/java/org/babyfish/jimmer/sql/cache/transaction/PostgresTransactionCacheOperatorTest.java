package org.babyfish.jimmer.sql.cache.transaction;

import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;

import javax.sql.DataSource;

public class PostgresTransactionCacheOperatorTest extends AbstractTransactionCacheOperatorTest {

    @Override
    protected void assume() {
        NativeDatabases.assumeNativeDatabase();
    }

    @Override
    protected DataSource dataSource() {
        return NativeDatabases.POSTGRES_DATA_SOURCE;
    }

    @Override
    protected Dialect dialect() {
        return new PostgresDialect();
    }
}
