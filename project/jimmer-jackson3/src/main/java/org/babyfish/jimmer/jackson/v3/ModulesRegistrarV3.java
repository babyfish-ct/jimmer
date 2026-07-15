package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.cfg.MapperBuilder;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.ModulesRegistrarV3}。
 */
@Deprecated
public class ModulesRegistrarV3 extends org.babyfish.jimmer.json.jackson.v3.ModulesRegistrarV3 {

    public static void registerImmutableModule(MapperBuilder<?, ?> builder) {
        org.babyfish.jimmer.json.jackson.v3.ModulesRegistrarV3.registerImmutableModule(builder);
    }

    public static void registerWellKnownModules(MapperBuilder<?, ?> builder) {
        org.babyfish.jimmer.json.jackson.v3.ModulesRegistrarV3.registerWellKnownModules(builder);
    }
}
