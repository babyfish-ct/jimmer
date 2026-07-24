package org.babyfish.jimmer.sql.transaction;

import org.babyfish.jimmer.sql.JSqlClient;

@TargetAnnotation(Component.class)
@Tx(Propagation.MANDATORY)
public class ServiceA {

    protected final JSqlClient sqlClient;

    public ServiceA(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public int a() {
        return 0;
    }

    @Tx(Propagation.REQUIRES_NEW)
    void b() {}
}
