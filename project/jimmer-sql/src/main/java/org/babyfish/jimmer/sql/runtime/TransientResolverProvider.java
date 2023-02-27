package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.Transient;
import org.babyfish.jimmer.sql.TransientResolver;

public interface TransientResolverProvider {

    TransientResolver<?, ?> get(
            Class<TransientResolver<?, ?>> resolverType,
            JSqlClient sqlClient
    ) throws Exception;

    default TransientResolver<?, ?> get(String ref) throws Exception {
        throw new UnsupportedOperationException(
                "The `ref` of \"@" +
                        Transient.class.getName() +
                        "\" is not supported by \"" +
                        getClass().getName() +
                        "\""
        );
    }

    default boolean shouldResolversCreatedImmediately() {
        return false;
    }
}
