package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.module.SimpleModule;

public class ImmutableModule3 extends SimpleModule {

    public static final String MODULE_ID = ImmutableModule3.class.getName();

    @Override
    public String getModuleName() {
        return MODULE_ID;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addSerializerModifier(new ImmutableSerializerModifier3());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospector3());
    }
}
