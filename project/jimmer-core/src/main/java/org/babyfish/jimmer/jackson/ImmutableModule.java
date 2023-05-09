package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ImmutableModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.setClassIntrospector(new JimmerClassIntrospector());
        ctx.addSerializers(new ImmutableSerializers());
        ctx.addDeserializers(new ImmutableDeserializers());
    }
}
