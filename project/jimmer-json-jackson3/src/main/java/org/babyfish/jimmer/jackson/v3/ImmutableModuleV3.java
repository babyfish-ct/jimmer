package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.module.SimpleModule;

public class ImmutableModuleV3 extends SimpleModule {

    public static final String MODULE_ID = ImmutableModuleV3.class.getName();

    @Override
    public Object getRegistrationId() {
        return MODULE_ID;
    }

    @Override
    public String getModuleName() {
        return MODULE_ID;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addSerializerModifier(new ImmutableSerializerModifierV3());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospectorV3());
    }
}
