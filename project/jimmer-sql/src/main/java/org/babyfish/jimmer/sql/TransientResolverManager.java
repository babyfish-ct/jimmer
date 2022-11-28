package org.babyfish.jimmer.sql;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.reflect.*;
import java.util.Map;

class TransientResolverManager {

    private static final Class<?> K_SQL_CLIENT_CLASS;

    private static final Constructor<?> K_SQL_CLIENT_IMPL_CONSTRUCTOR;

    private final JSqlClient owner;

    private final StaticCache<ImmutableProp, TransientResolver<?, ?>> resolverCache =
            new StaticCache<>(this::createResolver, true);

    TransientResolverManager(JSqlClient owner) {
        this.owner = owner;
    }

    public TransientResolver<?, ?> get(ImmutableProp prop) {
        return resolverCache.get(prop);
    }

    private TransientResolver<?, ?> createResolver(ImmutableProp prop) {
        if (!prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("\"" + prop + "\" is not declared in entity");
        }
        Transient trans = prop.getAnnotation(Transient.class);
        if (trans == null) {
            return null;
        }
        Class<?> resolverType = trans.value();
        if (resolverType == void.class) {
            return null;
        }
        if (!TransientResolver.class.isAssignableFrom(trans.value())) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the resolver type must implement \"" +
                            TransientResolver.class +
                            "\""
            );
        }
        if (resolverType.isInterface() || (resolverType.getModifiers() & Modifier.ABSTRACT) != 0) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the resolver type must be non-abstract class"
            );
        }
        if (resolverType.getTypeParameters().length != 0) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the resolver type cannot has parameters"
            );
        }
        Map<TypeVariable<?>, Type> typeMap =
                TypeUtils.getTypeArguments(resolverType, TransientResolver.class);
        Type keyType = typeMap.get(TransientResolver.class.getTypeParameters()[0]);
        Type valueType = typeMap.get(TransientResolver.class.getTypeParameters()[1]);
        if (!(keyType instanceof Class<?>) ||
                !Classes.matches((Class<?>)keyType, prop.getDeclaringType().getIdProp().getElementClass())
        ) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the first generic type argument of resolver type must be \"" +
                            prop.getDeclaringType().getIdProp().getElementClass() +
                            "\""
            );
        }
        if (!(valueType instanceof Class<?>) ||
                !Classes.matches((Class<?>)valueType, prop.getElementClass())) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the second generic type argument of resolver type must be \"" +
                            prop.getElementClass() +
                            "\""
            );
        }

        Constructor<?> constructor = null;

        try {
            constructor = resolverType.getConstructor();
        } catch (NoSuchMethodException ex) {
        }
        if (constructor != null) {
            try {
                return (TransientResolver<?, ?>) constructor.newInstance();
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                throw convertResolverConstructorError(prop, ex);
            }
        }

        try {
            constructor = resolverType.getConstructor(JSqlClient.class);
        } catch (NoSuchMethodException ex) {
        }
        if (constructor != null) {
            try {
                return (TransientResolver<?, ?>) constructor.newInstance(owner);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                throw convertResolverConstructorError(prop, ex);
            }
        }

        if (K_SQL_CLIENT_CLASS != null) {
            try {
                constructor = resolverType.getConstructor(K_SQL_CLIENT_CLASS);
            } catch (NoSuchMethodException ex) {
            }
            if (constructor != null) {
                Object kSqlClient;
                try {
                    kSqlClient = K_SQL_CLIENT_IMPL_CONSTRUCTOR.newInstance(owner);
                } catch (InvocationTargetException ex) {
                    throw new AssertionError("Internal bug", ex.getTargetException());
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new AssertionError("Internal bug", ex);
                }
                try {
                    return (TransientResolver<?, ?>) constructor.newInstance(kSqlClient);
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                    throw convertResolverConstructorError(prop, ex);
                }
            }
        }
        throw new ModelException(
                "The resolve type \"" +
                        resolverType.getName() +
                        "\" for the association \"" +
                        prop +
                        "\" does not have no-argument constructor or constructor that accepts SqlClient"
        );
    }

    private static RuntimeException convertResolverConstructorError(ImmutableProp prop, Throwable throwable) {
        if (throwable instanceof InvocationTargetException) {
            throwable = ((InvocationTargetException)throwable).getTargetException();
        }
        return new ModelException(
                "Cannot create resolver \"" +
                        prop.getAnnotation(Transient.class).value() +
                        "\" for property \"" +
                        prop +
                        "\"",
                throwable
        );
    }

    static {

        Class<?> kSqlClientClass = null;
        Class<?> kSqlClientImplClass = null;
        Constructor<?> kSqlClientImplConstructor = null;

        try {
            kSqlClientClass = Class.forName("org.babyfish.jimmer.sql.kt.KSqlClient");
        } catch (ClassNotFoundException ex) {
        }

        if (kSqlClientClass != null) {
            try {
                kSqlClientImplClass = Class.forName("org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl");
                kSqlClientImplConstructor = kSqlClientImplClass.getConstructor(JSqlClient.class);
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                throw new AssertionError("Internal bug", ex);
            }
        }

        K_SQL_CLIENT_CLASS = kSqlClientClass;
        K_SQL_CLIENT_IMPL_CONSTRUCTOR = kSqlClientImplConstructor;
    }
}
