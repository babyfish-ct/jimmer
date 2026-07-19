package org.babyfish.jimmer.jackson.codec;

import java.util.Map;

public class SharedAttributesCustomization implements JsonCodecCustomization {
    private final Map<?, ?> attributes;

    public SharedAttributesCustomization(Map<?, ?> attributes) {
        this.attributes = attributes;
    }

    @Override
    public void customizeV2(com.fasterxml.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        builder.defaultAttributes(com.fasterxml.jackson.databind.cfg.ContextAttributes.getEmpty()
                .withSharedAttributes(attributes));
    }

    @Override
    public void customizeV3(tools.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        builder.defaultAttributes(tools.jackson.databind.cfg.ContextAttributes.getEmpty()
                .withSharedAttributes(attributes));
    }
}
