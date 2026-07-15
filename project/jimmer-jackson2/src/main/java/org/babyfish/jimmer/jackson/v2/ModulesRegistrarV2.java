package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.cfg.MapperBuilder;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.ModulesRegistrarV2}。
 */
@Deprecated
public class ModulesRegistrarV2 extends org.babyfish.jimmer.json.jackson.v2.ModulesRegistrarV2 {

    public static void registerImmutableModule(MapperBuilder<?, ?> builder) {
        org.babyfish.jimmer.json.jackson.v2.ModulesRegistrarV2.registerImmutableModule(builder);
    }

    public static void registerWellKnownModules(MapperBuilder<?, ?> builder) {
        org.babyfish.jimmer.json.jackson.v2.ModulesRegistrarV2.registerWellKnownModules(builder);
    }
}
