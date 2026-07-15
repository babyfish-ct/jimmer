package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.codec.JsonCodecCustomizationTarget;
import org.babyfish.jimmer.json.codec.JsonCodecFamily;
import org.babyfish.jimmer.json.codec.PropertyNamingCustomization;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.ContextAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

class JsonCodecCustomizationTargetV3 implements JsonCodecCustomizationTarget {

    private final JsonMapper.Builder builder;

    JsonCodecCustomizationTargetV3(JsonMapper.Builder builder) {
        this.builder = builder;
    }

    @Override
    public JsonCodecFamily family() {
        return JsonCodecFamily.JACKSON3;
    }

    @Override
    public void registerImmutableModule() {
        ModulesRegistrarV3.registerImmutableModule(builder);
    }

    @Override
    public void setPropertyNaming(PropertyNamingCustomization.PropertyNaming propertyNaming) {
        switch (propertyNaming) {
            case LOWER_CAMEL_CASE:
                builder.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
                break;
            case UPPER_CAMEL_CASE:
                builder.propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
                break;
            case LOWER_CASE:
                builder.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
                break;
            case SNAKE_CASE:
                builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
                break;
            case KEBAB_CASE:
                builder.propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
                break;
            case LOWER_DOT_CASE:
                builder.propertyNamingStrategy(PropertyNamingStrategies.LOWER_DOT_CASE);
                break;
        }
    }

    @Override
    public void setSharedAttributes(Map<?, ?> attributes) {
        builder.defaultAttributes(ContextAttributes.getEmpty().withSharedAttributes(attributes));
    }

    @Override
    public void addNativeModule(Object module) {
        builder.addModule((JacksonModule) module);
    }
}
