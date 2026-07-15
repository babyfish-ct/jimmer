package org.babyfish.jimmer.json.codec;

import java.util.ServiceLoader;

class JsonCodecDetector {
    static final JsonCodec JSON_CODEC;

    static {
        JsonCodecProvider provider = loadJsonCodecProvider();
        JSON_CODEC = provider.create().withCustomizations(new ImmutableModuleCustomization());
    }

    static JsonCodecProvider loadJsonCodecProvider() {
        JsonCodecProvider provider = loadJsonCodecProvider(ServiceLoader.load(JsonCodecProvider.class), null);
        provider = loadJsonCodecProvider(
                ServiceLoader.load(JsonCodecProvider.class, JsonCodecProvider.class.getClassLoader()),
                provider
        );
        if (provider != null) {
            return provider;
        }
        throw new IllegalStateException(
                "No JSON codec provider is in classpath, please add a JSON codec provider module"
        );
    }

    private static JsonCodecProvider loadJsonCodecProvider(
            ServiceLoader<JsonCodecProvider> serviceLoader,
            JsonCodecProvider selected
    ) {
        for (JsonCodecProvider provider : serviceLoader) {
            if (selected == null || provider.priority() > selected.priority()) {
                selected = provider;
            }
        }
        return selected;
    }
}
