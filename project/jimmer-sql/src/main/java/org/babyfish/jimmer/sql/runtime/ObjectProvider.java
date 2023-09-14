package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.Transient;
import org.babyfish.jimmer.sql.TransientResolver;

import java.lang.reflect.Constructor;

public interface ObjectProvider<T> {

    @SuppressWarnings("unchecked")
    default T get(
            Class<T> type,
            JSqlClient sqlClient
    ) throws Exception {
        Constructor<?> constructor = null;
        try {
            constructor = type.getConstructor();
        } catch (NoSuchMethodException ex) {
            // Do nothing
        }
        if (constructor != null) {
            return (T) constructor.newInstance();
        }
        throw new IllegalArgumentException(
                "The type \"" +
                        type.getName() +
                        "\" does not support default constructor"
        );
    }

    default T get(String ref, JSqlClient sqlClient) throws Exception {
        throw new UnsupportedOperationException(
                "The `ref` " +
                        "\" is not supported by \"" +
                        getClass().getName() +
                        "\""
        );
    }

    default boolean shouldResolversCreatedImmediately() {
        return false;
    }
}
