package org.babyfish.jimmer.json.codec;

public class ImmutableModuleCustomization implements JsonCodecCustomization {
    @Override
    public void customize(JsonCodecCustomizationTarget target) {
        target.registerImmutableModule();
    }
}
