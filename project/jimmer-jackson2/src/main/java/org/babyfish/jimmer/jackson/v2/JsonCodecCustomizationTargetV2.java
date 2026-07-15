package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import org.babyfish.jimmer.json.codec.JsonCodecCustomizationTarget;
import org.babyfish.jimmer.json.codec.JsonCodecFamily;
import org.babyfish.jimmer.json.codec.PropertyNamingCustomization;

import java.util.Map;

class JsonCodecCustomizationTargetV2 implements JsonCodecCustomizationTarget {

    private final ObjectMapperBuilder builder;

    JsonCodecCustomizationTargetV2(ObjectMapperBuilder builder) {
        this.builder = builder;
    }

    @Override
    public JsonCodecFamily family() {
        return JsonCodecFamily.JACKSON2;
    }

    @Override
    public void registerImmutableModule() {
        ModulesRegistrarV2.registerImmutableModule(builder);
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
        builder.addModule((Module) module);
    }
}
