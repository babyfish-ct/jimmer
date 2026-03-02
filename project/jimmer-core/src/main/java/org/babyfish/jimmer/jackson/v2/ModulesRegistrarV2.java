package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import static org.babyfish.jimmer.jackson.ClassUtils.classExists;

/**
 * All module registrations must be wrapped with separate classes to avoid {@link ClassNotFoundException} in apt and ksp modules.
 */
public class ModulesRegistrarV2 {
    public static void registerImmutableModule(MapperBuilder<?, ?> builder) {
        ImmutableModuleRegistrar.register(builder);
    }

    public static void registerWellKnownModules(MapperBuilder<?, ?> builder) {
        if (classExists("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule")) {
            JavaTimeModuleRegistrar.register(builder);
        }
        if (classExists("com.fasterxml.jackson.module.kotlin.KotlinModule")) {
            KotlinModuleRegistrar.register(builder);
        }
    }

    private static class ImmutableModuleRegistrar {
        private static void register(MapperBuilder<?, ?> builder) {
            builder.addModule(new ImmutableModuleV2());
        }
    }

    private static class JavaTimeModuleRegistrar {
        private static void register(MapperBuilder<?, ?> builder) {
            builder.addModule(new JavaTimeModule());
        }
    }

    private static class KotlinModuleRegistrar {
        private static void register(MapperBuilder<?, ?> builder) {
            builder.addModule(new KotlinModule.Builder().build());
        }
    }
}
