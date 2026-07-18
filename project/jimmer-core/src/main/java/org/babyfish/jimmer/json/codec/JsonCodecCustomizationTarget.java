package org.babyfish.jimmer.json.codec;

import java.util.Map;

public interface JsonCodecCustomizationTarget {

    void registerImmutableModule();

    void setPropertyNaming(PropertyNamingCustomization.PropertyNaming propertyNaming);

    void setSharedAttributes(Map<?, ?> attributes);

    boolean acceptsNativeModule(Class<?> moduleType);

    void addNativeModule(Object module);
}
