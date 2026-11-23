package org.babyfish.jimmer.json;

import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.*;

public class JsonCodecsClassLoaderTest {

    @Test
    public void contextLoadersShareCoreAndKeepTheirOwnDefaultCodec() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader jackson2 = adapterLoader(JsonCodecV2.class);
             URLClassLoader jackson3 = adapterLoader(JsonCodecV3.class)) {
            thread.setContextClassLoader(jackson2);
            JsonCodec first = JsonCodecs.defaultCodec();
            assertSame(jackson2, first.getClass().getClassLoader());
            assertEquals(JsonCodecV2.class.getName(), first.getClass().getName());

            thread.setContextClassLoader(jackson3);
            JsonCodec second = JsonCodecs.defaultCodec();
            assertSame(jackson3, second.getClass().getClassLoader());
            assertEquals(JsonCodecV3.class.getName(), second.getClass().getName());
            assertSame(first.getClass(), JsonCodecs.create(JsonCodecV2.class.getName(), jackson2).getClass());

            thread.setContextClassLoader(jackson2);
            assertSame(first, JsonCodecs.defaultCodec());
            thread.setContextClassLoader(jackson3);
            assertSame(second, JsonCodecs.defaultCodec());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    public void explicitCodecNeedsNoRegistration() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader loader = adapterLoader(GsonCodec.class)) {
            thread.setContextClassLoader(loader);
            JsonCodec codec = JsonCodecs.create(GsonCodec.class.getName());
            assertSame(loader, codec.getClass().getClassLoader());
            assertEquals("42", codec.writer().writeAsString(42));
            assertThrows(IllegalStateException.class, JsonCodecs::defaultCodec);
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    public void nullContextLoaderUsesCoreClassLoader() {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(JsonCodecs.class.getClassLoader());
            JsonCodec expected = JsonCodecs.defaultCodec();
            thread.setContextClassLoader(null);
            assertSame(expected, JsonCodecs.defaultCodec());
            assertEquals(JsonCodecV3.class, expected.getClass());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private static URLClassLoader adapterLoader(Class<? extends JsonCodec> codec) {
        ClassLoader parent = new ClassLoader(JsonCodecs.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // Delegate engines and core to the shared parent, load adapters only from the child's classpath.
                if (name.startsWith("org.babyfish.jimmer.jackson.v2.") ||
                        name.startsWith("org.babyfish.jimmer.jackson.v3.") ||
                        name.startsWith("org.babyfish.jimmer.json.gson.")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        URL url = codec.getProtectionDomain().getCodeSource().getLocation();
        return new URLClassLoader(new URL[] { url }, parent);
    }
}
