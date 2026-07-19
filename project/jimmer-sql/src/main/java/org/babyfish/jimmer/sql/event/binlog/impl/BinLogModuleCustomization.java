package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.jackson.codec.JsonCodecCustomization;

public class BinLogModuleCustomization implements JsonCodecCustomization {
    private final BinLogParser parser;

    public BinLogModuleCustomization(BinLogParser parser) {
        this.parser = parser;
    }

    @Override
    public void customizeV2(com.fasterxml.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        BinLogModuleRegistrarV2.register(builder, parser);
    }

    @Override
    public void customizeV3(tools.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        BinLogModuleRegistrarV3.register(builder, parser);
    }

    private static class BinLogModuleRegistrarV2 {
        private static void register(com.fasterxml.jackson.databind.cfg.MapperBuilder<?, ?> builder, BinLogParser parser) {
            builder.addModule(new BinLogModuleV2(parser));
        }
    }

    private static class BinLogModuleRegistrarV3 {
        private static void register(tools.jackson.databind.cfg.MapperBuilder<?, ?> builder, BinLogParser parser) {
            builder.addModule(new BinLogModuleV3(parser));
        }
    }
}
