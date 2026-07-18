package org.babyfish.jimmer.json.codec;

public class PropertyNamingCustomization implements JsonCodecCustomization {
    private final PropertyNaming propertyNaming;

    public PropertyNamingCustomization(PropertyNaming propertyNaming) {
        this.propertyNaming = propertyNaming;
    }

    @Override
    public void customize(JsonCodecCustomizationTarget target) {
        target.setPropertyNaming(propertyNaming);
    }

    public enum PropertyNaming {
        LOWER_CAMEL_CASE,
        UPPER_CAMEL_CASE,
        LOWER_CASE,
        SNAKE_CASE,
        KEBAB_CASE,
        LOWER_DOT_CASE
    }
}
