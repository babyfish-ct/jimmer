package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.Transient;
import org.babyfish.jimmer.sql.TransientResolver;

import java.lang.reflect.Constructor;

public class DefaultTransientResolverProvider implements ObjectProvider<TransientResolver<?, ?>> {

    public static final DefaultTransientResolverProvider INSTANCE =
            new DefaultTransientResolverProvider();

    private static final Class<?> K_SQL_CLIENT_CLASS;

    private static final Constructor<?> K_SQL_CLIENT_IMPL_CONSTRUCTOR;

    protected DefaultTransientResolverProvider() {}

    @Override
    public TransientResolver<?, ?> get(
            Class<TransientResolver<?, ?>> type,
            JSqlClient sqlClient
    ) throws Exception {

        Constructor<?> constructor = null;
        try {
            constructor = type.getConstructor();
        } catch (NoSuchMethodException ex) {
            // Do nothing
        }
        if (constructor != null) {
            return (TransientResolver<?, ?>) constructor.newInstance();
        }

        try {
            constructor = type.getConstructor(JSqlClient.class);
        } catch (NoSuchMethodException ex) {
            // Do nothing
        }
        if (constructor != null) {
            return (TransientResolver<?, ?>) constructor.newInstance(sqlClient);
        }

        if (K_SQL_CLIENT_CLASS != null) {
            try {
                constructor = type.getConstructor(K_SQL_CLIENT_CLASS);
            } catch (NoSuchMethodException ex) {
                // Do nothing
            }
            if (constructor != null) {
                Object kSqlClient;
                kSqlClient = K_SQL_CLIENT_IMPL_CONSTRUCTOR.newInstance(sqlClient);
                return (TransientResolver<?, ?>) constructor.newInstance(kSqlClient);
            }
        }

        throw new IllegalArgumentException(
                "The resolve type \"" +
                        type.getName() +
                        "\", it does not have no-argument constructor or constructor accepts SqlClient"
        );
    }

    @Override
    public TransientResolver<?, ?> get(String ref, JSqlClient sqlClient) throws Exception {
        throw new UnsupportedOperationException(
                "The `ref` of \"@" +
                        Transient.class.getName() +
                        "\" is not supported by \"" +
                        getClass().getName() +
                        "\""
        );
    }

    @Override
    public boolean shouldResolversCreatedImmediately() {
        return true;
    }

    static {

        Class<?> kSqlClientClass = null;
        Class<?> kSqlClientImplClass;
        Constructor<?> kSqlClientImplConstructor = null;

        try {
            kSqlClientClass = Class.forName("org.babyfish.jimmer.sql.kt.KSqlClient");
        } catch (ClassNotFoundException ex) {
        }

        if (kSqlClientClass != null) {
            try {
                kSqlClientImplClass = Class.forName("org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl");
                kSqlClientImplConstructor = kSqlClientImplClass.getConstructor(JSqlClientImplementor.class);
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                throw new AssertionError("Internal bug", ex);
            }
        }

        K_SQL_CLIENT_CLASS = kSqlClientClass;
        K_SQL_CLIENT_IMPL_CONSTRUCTOR = kSqlClientImplConstructor;
    }
}
