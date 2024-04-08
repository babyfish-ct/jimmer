package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ImmutableModule extends SimpleModule {

    private static final Object JIMMER_MODULE_ID = new Object();

    @Override
    public Object getTypeId() {
        return JIMMER_MODULE_ID;
    }

    @Override
    public String getModuleName() {
        return "jimmer-module";
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addBeanSerializerModifier(new ImmutableSerializerModifier());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospector());
    }
}
