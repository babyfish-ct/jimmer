package org.babyfish.jimmer.json.codec;

import java.util.Map;

public class SharedAttributesCustomization implements JsonCodecCustomization {
    private final Map<?, ?> attributes;

    public SharedAttributesCustomization(Map<?, ?> attributes) {
        this.attributes = attributes;
    }

    @Override
    public void customize(JsonCodecCustomizationTarget target) {
        target.setSharedAttributes(attributes);
    }
}
