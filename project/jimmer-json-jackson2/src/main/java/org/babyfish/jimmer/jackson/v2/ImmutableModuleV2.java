package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ImmutableModuleV2 extends SimpleModule {

    public static final String MODULE_ID = ImmutableModuleV2.class.getName();

    @Override
    public Object getTypeId() {
        return MODULE_ID;
    }

    @Override
    public String getModuleName() {
        return MODULE_ID;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addBeanSerializerModifier(new ImmutableSerializerModifierV2());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospectorV2());
    }
}
