package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.databind.module.SimpleModule;

class BinLogModule extends SimpleModule {

    private final BinLogParser parser;

    BinLogModule(BinLogParser parser) {
        this.parser = parser;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addDeserializers(new BinLogDeserializers(parser));
    }
}
