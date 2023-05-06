package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

class BinLogModule extends SimpleModule {

    private final JSqlClientImplementor sqlClient;

    BinLogModule(JSqlClientImplementor sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addDeserializers(new BinLogDeserializers(sqlClient));
    }
}
