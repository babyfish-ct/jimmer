package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.Map;

class BinLogModule extends SimpleModule {

    private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    BinLogModule(Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap) {
        this.scalarProviderMap = scalarProviderMap;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addDeserializers(new BinLogDeserializers(scalarProviderMap));
    }
}
