package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ImmutableModule extends SimpleModule {

    public static final String MODULE_ID = ImmutableModule.class.getName();

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
        ctx.addBeanSerializerModifier(new ImmutableSerializerModifier());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospector());
    }
}
