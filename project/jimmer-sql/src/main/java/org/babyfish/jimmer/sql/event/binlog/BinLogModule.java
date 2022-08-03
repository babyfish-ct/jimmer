package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.module.SimpleModule;

class BinLogModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addDeserializers(new BinLogDeserializers());
    }
}
