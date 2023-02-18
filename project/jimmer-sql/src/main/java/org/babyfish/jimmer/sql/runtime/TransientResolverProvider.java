package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;

public interface TransientResolverProvider {

    TransientResolver<?, ?> get(
            Class<TransientResolver<?, ?>> resolverType,
            JSqlClient sqlClient
    ) throws Exception;

    default boolean shouldResolversCreatedImmediately() {
        return false;
    }
}
