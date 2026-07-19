package org.babyfish.jimmer.jackson.codec;

public class PropertyNamingCustomization implements JsonCodecCustomization {
    private final PropertyNaming propertyNaming;

    public PropertyNamingCustomization(PropertyNaming propertyNaming) {
        this.propertyNaming = propertyNaming;
    }

    @Override
    public void customizeV2(com.fasterxml.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        switch (propertyNaming) {
            case LOWER_CAMEL_CASE:
                builder.propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE);
                break;
            case UPPER_CAMEL_CASE:
                builder.propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE);
                break;
            case LOWER_CASE:
                builder.propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CASE);
                break;
            case SNAKE_CASE:
                builder.propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
                break;
            case KEBAB_CASE:
                builder.propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE);
                break;
            case LOWER_DOT_CASE:
                builder.propertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_DOT_CASE);
                break;
        }
    }

    @Override
    public void customizeV3(tools.jackson.databind.cfg.MapperBuilder<?, ?> builder) {
        switch (propertyNaming) {
            case LOWER_CAMEL_CASE:
                builder.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE);
                break;
            case UPPER_CAMEL_CASE:
                builder.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE);
                break;
            case LOWER_CASE:
                builder.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.LOWER_CASE);
                break;
            case SNAKE_CASE:
                builder.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
                break;
            case KEBAB_CASE:
                builder.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.KEBAB_CASE);
                break;
            case LOWER_DOT_CASE:
                builder.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.LOWER_DOT_CASE);
                break;
        }
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
