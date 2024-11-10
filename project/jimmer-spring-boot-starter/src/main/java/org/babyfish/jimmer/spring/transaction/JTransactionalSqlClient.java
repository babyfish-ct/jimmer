package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.di.AbstractJSqlClientDelegate;
import org.babyfish.jimmer.sql.runtime.*;

class JTransactionalSqlClient extends AbstractJSqlClientDelegate {

    private static final EntityManager EMPTY_ENTITY_MANAGER = new EntityManager();

    @Override
    protected JSqlClientImplementor sqlClient() {
        JSqlClient sqlClient = JimmerTransactionManager.sqlClient();
        if (sqlClient == null) {
            throw new IllegalStateException(
                    "The transactional sql client is used, " +
                            "however, there is no AOP transaction, or the transaction manager is not \"" +
                            JimmerTransactionManager.class.getName() +
                            "\""
            );
        }
        return (JSqlClientImplementor) sqlClient;
    }

    @Override
    public void initialize() {}

    @Override
    public EntityManager getEntityManager() {
        return EMPTY_ENTITY_MANAGER;
    }
}
