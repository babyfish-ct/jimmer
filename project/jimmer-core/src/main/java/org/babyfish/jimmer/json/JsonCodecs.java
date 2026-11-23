package org.babyfish.jimmer.json;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Creates codecs by implementation class and supplies the default for calls without a codec. */
public final class JsonCodecs {

    private static final String JACKSON_3_CODEC = "org.babyfish.jimmer.jackson.v3.JsonCodecV3";

    private static final String JACKSON_2_CODEC = "org.babyfish.jimmer.jackson.v2.JsonCodecV2";

    // A mapper can retain its application's model classes, and through them its class loader.
    private static final Map<ClassLoader, WeakReference<JsonCodec>> DEFAULT_CODECS = new WeakHashMap<>();

    private static volatile JsonCodec localDefault;

    private JsonCodecs() {}

    public static JsonCodec create(String className) {
        return create(className, contextClassLoader());
    }

    /** Loads the named implementation from the caller's plugin or processor classpath. */
    public static JsonCodec create(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader).asSubclass(JsonCodec.class).getConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException | LinkageError ex) {
            throw new IllegalStateException(
                    "Cannot create JSON codec \"" + className +
                            "\". Specify a JsonCodec implementation with a public no-argument constructor.", ex
            );
        }
    }

    /** Returns the default Jackson codec visible to the current thread's context class loader. */
    public static JsonCodec defaultCodec() {
        ClassLoader loader = contextClassLoader();
        JsonCodec codec = localDefault;
        if (loader == JsonCodecs.class.getClassLoader() && codec != null) {
            return codec;
        }
        return getOrCreateDefault(loader);
    }

    private static synchronized JsonCodec getOrCreateDefault(ClassLoader loader) {
        if (loader == JsonCodecs.class.getClassLoader()) {
            if (localDefault == null) {
                localDefault = createDefault(loader);
            }
            return localDefault;
        }
        WeakReference<JsonCodec> ref = DEFAULT_CODECS.get(loader);
        JsonCodec codec = ref != null ? ref.get() : null;
        if (codec == null) {
            codec = createDefault(loader);
            DEFAULT_CODECS.put(loader, new WeakReference<>(codec));
        }
        return codec;
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassLoader localLoader = JsonCodecs.class.getClassLoader();
        if (loader != null && loader != localLoader) {
            try {
                if (loader.loadClass(JsonCodec.class.getName()) == JsonCodec.class) {
                    return loader;
                }
            } catch (ClassNotFoundException ex) {
                // The context loader can be used solely for locating schema resources.
            }
        }
        return localLoader;
    }

    private static JsonCodec createDefault(ClassLoader loader) {
        if (isPresent("tools.jackson.databind.ObjectMapper", loader) &&
                isPresent(JACKSON_3_CODEC, loader)) {
            return create(JACKSON_3_CODEC, loader);
        }
        if (isPresent("com.fasterxml.jackson.databind.ObjectMapper", loader) &&
                isPresent(JACKSON_2_CODEC, loader)) {
            return create(JACKSON_2_CODEC, loader);
        }
        throw new IllegalStateException(
                "No default Jackson codec is available. Pass a JsonCodec explicitly or install a Jimmer Jackson adapter."
        );
    }

    private static boolean isPresent(String className, ClassLoader loader) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
