package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.babyfish.jimmer.jackson.impl.ImmutableDeserializers;
import org.babyfish.jimmer.jackson.impl.ImmutableSerializers;
import org.babyfish.jimmer.jackson.impl.JimmerClassIntrospector;

public class ImmutableModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.setClassIntrospector(new JimmerClassIntrospector());
        ctx.addSerializers(new ImmutableSerializers());
        ctx.addDeserializers(new ImmutableDeserializers());
    }
}
