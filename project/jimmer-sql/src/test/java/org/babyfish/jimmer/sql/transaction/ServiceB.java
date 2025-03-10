package org.babyfish.jimmer.sql.transaction;

import org.babyfish.jimmer.sql.JSqlClient;

@TargetAnnotation(Component.class)
public class ServiceB {

    protected final JSqlClient sqlClient;

    public ServiceB(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Tx
    public void a() {}

    @Tx(Propagation.REQUIRES_NEW)
    public void b() {}
}
