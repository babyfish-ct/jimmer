package org.babyfish.jimmer.json.codec;

import java.util.Map;

public interface JsonCodecCustomizationTarget {

    JsonCodecFamily family();

    void registerImmutableModule();

    void setPropertyNaming(PropertyNamingCustomization.PropertyNaming propertyNaming);

    void setSharedAttributes(Map<?, ?> attributes);

    void addNativeModule(Object module);
}
