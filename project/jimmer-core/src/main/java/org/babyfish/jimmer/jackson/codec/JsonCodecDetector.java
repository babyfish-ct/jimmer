package org.babyfish.jimmer.jackson.codec;

import org.babyfish.jimmer.jackson.v2.JsonCodecProviderV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecProviderV3;

import java.util.Iterator;
import java.util.ServiceLoader;

import static org.babyfish.jimmer.jackson.ClassUtils.classExists;

class JsonCodecDetector {
    static final JsonCodec<?> JSON_CODEC;

    static {
        JsonCodecProvider provider = loadJsonCodecProvider();
        JSON_CODEC = provider.create().withCustomizations(new ImmutableModuleCustomization());
    }

    static JsonCodecProvider loadJsonCodecProvider() {
        ServiceLoader<JsonCodecProvider> serviceLoader = ServiceLoader.load(JsonCodecProvider.class);
        Iterator<JsonCodecProvider> providerIterator = serviceLoader.iterator();
        if (providerIterator.hasNext()) {
            return providerIterator.next();
        } else if (classExists("tools.jackson.databind.ObjectMapper")) {
            return new JsonCodecProviderV3();
        } else if (classExists("com.fasterxml.jackson.databind.ObjectMapper")) {
            return new JsonCodecProviderV2();
        } else {
            throw new IllegalStateException("Jackson is not in classpath");
        }
    }
}
