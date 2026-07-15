package org.babyfish.jimmer.json.codec;

import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

class JsonCodecDetector {
    static final JsonCodec JSON_CODEC;

    static {
        JsonCodecProvider provider = loadJsonCodecProvider();
        JSON_CODEC = provider.create().withCustomizations(new ImmutableModuleCustomization());
    }

    static JsonCodecProvider loadJsonCodecProvider() {
        Map<JsonCodecFamily, JsonCodecProvider> providerMap = new EnumMap<>(JsonCodecFamily.class);
        loadJsonCodecProviders(ServiceLoader.load(JsonCodecProvider.class), providerMap);
        loadJsonCodecProviders(
                ServiceLoader.load(JsonCodecProvider.class, JsonCodecProvider.class.getClassLoader()),
                providerMap
        );
        for (JsonCodecFamily family : new JsonCodecFamily[]{
                JsonCodecFamily.JACKSON3,
                JsonCodecFamily.JACKSON2,
                JsonCodecFamily.KOTLINX_SERIALIZATION
        }) {
            JsonCodecProvider provider = providerMap.get(family);
            if (provider != null) {
                return provider;
            }
        }
        throw new IllegalStateException(
                "No JSON codec provider is in classpath, please add jimmer-jackson2, jimmer-jackson3 or jimmer-kotlinx-serialization"
        );
    }

    private static void loadJsonCodecProviders(
            ServiceLoader<JsonCodecProvider> serviceLoader,
            Map<JsonCodecFamily, JsonCodecProvider> providerMap
    ) {
        for (JsonCodecProvider provider : serviceLoader) {
            providerMap.putIfAbsent(provider.family(), provider);
        }
    }
}
