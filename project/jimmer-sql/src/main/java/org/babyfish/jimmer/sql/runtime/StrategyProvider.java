package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;

import java.lang.reflect.Constructor;

public interface StrategyProvider<T> {

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
                "Illegal type \"" +
                        type.getName() +
                        "\", it is not manged by IOC framework but does not support default constructor"
        );
    }

    default T get(String ref, JSqlClient sqlClient) throws Exception {
        throw new UnsupportedOperationException(
                "The `ref` " +
                        "\" is not supported by \"" +
                        getClass().getName() +
                        "\" which is not used to support IOC framework"
        );
    }
}
