package org.babyfish.jimmer.jackson.codec;

import org.babyfish.jimmer.jackson.v2.ModulesRegistrarV2;
import org.babyfish.jimmer.jackson.v3.ModulesRegistrarV3;

public class ImmutableModuleCustomization implements JsonCodecCustomization {
    @Override
    public void customizeV2(com.fasterxml.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        ModulesRegistrarV2.registerImmutableModule(builder);
    }

    @Override
    public void customizeV3(tools.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        ModulesRegistrarV3.registerImmutableModule(builder);
    }
}
