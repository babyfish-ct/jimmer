package org.babyfish.jimmer.sql.cache.transaction;

import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;

import javax.sql.DataSource;

public class MySqlTransactionCacheOperatorTest extends AbstractTransactionCacheOperatorTest {

    @Override
    protected void assume() {
        NativeDatabases.assumeNativeDatabase();
    }

    @Override
    protected DataSource dataSource() {
        return NativeDatabases.MYSQL_DATA_SOURCE;
    }

    @Override
    protected Dialect dialect() {
        return new MySqlDialect();
    }
}
