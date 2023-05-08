//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ImmutableModule extends SimpleModule {
    public ImmutableModule() {
    }

    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.setClassIntrospector(new JimmerClassIntrospector());
        ctx.addSerializers(new ImmutableSerializers());
        ctx.addDeserializers(new ImmutableDeserializers());
    }
}
