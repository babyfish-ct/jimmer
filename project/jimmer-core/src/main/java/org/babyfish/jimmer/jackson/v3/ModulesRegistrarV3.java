package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.module.kotlin.KotlinFeature;
import tools.jackson.module.kotlin.KotlinModule;

import static org.babyfish.jimmer.jackson.ClassUtils.classExists;

/**
 * All module registrations must be wrapped with separate classes to avoid {@link ClassNotFoundException} in apt and ksp modules.
 */
public class ModulesRegistrarV3 {
    public static void registerImmutableModule(MapperBuilder<?, ?> builder) {
        ImmutableModuleRegistrar.register(builder);
    }

    public static void registerWellKnownModules(MapperBuilder<?, ?> builder) {
        if (classExists("tools.jackson.module.kotlin.KotlinModule")) {
            KotlinModuleRegistrar.register(builder);
        }
    }

    private static class ImmutableModuleRegistrar {
        private static void register(MapperBuilder<?, ?> builder) {
            builder.addModule(new ImmutableModuleV3());
        }
    }

    private static class KotlinModuleRegistrar {
        private static void register(MapperBuilder<?, ?> builder) {
            builder.addModule(new KotlinModule.Builder()
                    .enable(KotlinFeature.KotlinPropertyNameAsImplicitName) // for correct Tuples serialization
                    .build());
        }
    }
}
