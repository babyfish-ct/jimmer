package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.PaginationContext;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;

public class DynamicDialect implements Dialect {

    Dialect targetDialect;

    @Override
    public void paginate(PaginationContext ctx) {
        Dialect dialect = targetDialect != null ? targetDialect : DefaultDialect.INSTANCE;
        dialect.paginate(ctx);
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        Dialect dialect = targetDialect != null ? targetDialect : DefaultDialect.INSTANCE;
        return dialect.getUpdateJoin();
    }

    @Override
    public String getSelectIdFromSequenceSql(String String) {
        Dialect dialect = targetDialect != null ? targetDialect : DefaultDialect.INSTANCE;
        return dialect.getSelectIdFromSequenceSql(String);
    }

    @Override
    public String getLastIdentitySql() {
        Dialect dialect = targetDialect != null ? targetDialect : DefaultDialect.INSTANCE;
        return dialect.getLastIdentitySql();
    }

    @Override
    public String getOverrideIdentityIdSql() {
        Dialect dialect = targetDialect != null ? targetDialect : DefaultDialect.INSTANCE;
        return dialect.getOverrideIdentityIdSql();
    }
}
