package org.babyfish.jimmer.sql;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.runtime.TransientResolverProvider;

import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

class TransientResolverManager {

    private final TransientResolverProvider provider;

    private JSqlClient sqlClient;

    private final StaticCache<ImmutableProp, TransientResolver<?, ?>> resolverCache =
            new StaticCache<>(this::createResolver, true);

    TransientResolverManager(TransientResolverProvider provider) {
        this.provider = provider;
    }

    void initialize(JSqlClient sqlClient) {
        if (this.sqlClient != null) {
            throw new IllegalStateException("The current object has been initialized");
        }
        this.sqlClient = sqlClient;
        if (provider.shouldResolversCreatedImmediately()) {
            initializeForTrigger();
        }
    }

    /**
     * Resolvers must be created because resolver many register listeners
     */
    private void initializeForTrigger() {
        Caches caches = sqlClient.getCaches();
        if (caches != null) {
            // Important, initialize necessary resolvers
            for (ImmutableType type : ((CachesImpl)caches).getObjectCacheMap().keySet()) {
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.hasTransientResolver()) {
                        get(prop);
                    }
                }
            }
            for (ImmutableProp prop : ((CachesImpl)caches).getPropCacheMap().keySet()) {
                if (prop.hasTransientResolver()) {
                    get(prop);
                }
            }
        }
    }

    public TransientResolver<?, ?> get(ImmutableProp prop) {
        return resolverCache.get(prop);
    }

    public Class<? extends TransientResolverProvider> getProviderClass() {
        return provider.getClass();
    }

    @SuppressWarnings("unchecked")
    private TransientResolver<?, ?> createResolver(ImmutableProp prop) {
        if (!prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("\"" + prop + "\" is not declared in entity");
        }
        Transient trans = prop.getAnnotation(Transient.class);
        if (trans == null) {
            return null;
        }
        Class<?> resolverType = trans.value();
        String resolverRef = trans.ref();
        if (resolverType == void.class && resolverRef.isEmpty()) {
            return null;
        }
        TransientResolver<?, ?> resolver = null;
        if (!resolverRef.isEmpty()) {
            try {
                resolver = provider.get(resolverRef, sqlClient);
            } catch (Exception ex) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the \"" +
                                provider.getClass().getName() +
                                ".get(String)\" throws exception",
                        ex
                );
            }
            if (resolver == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the \"" +
                                provider.getClass().getName() +
                                ".get(String)\" returns null"
                );
            }
            resolverType = resolver.getClass();
        }
        if (!TransientResolver.class.isAssignableFrom(resolverType)) {
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
                !Classes.matches((Class<?>)keyType, prop.getDeclaringType().getIdProp().getElementClass())) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the first generic type argument of resolver type must be \"" +
                            prop.getDeclaringType().getIdProp().getElementClass() +
                            "\""
            );
        }
        if (prop.isAssociation(TargetLevel.ENTITY)) {
            ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
            if (prop.isReferenceList(TargetLevel.ENTITY)) {
                Type valueElementType = null;
                if (valueType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) valueType;
                    if (parameterizedType.getRawType() == List.class) {
                        valueElementType = parameterizedType.getActualTypeArguments()[0];
                        if (valueElementType instanceof WildcardType) {
                            valueElementType = ((WildcardType) valueElementType).getUpperBounds()[0];
                        }
                    }
                }
                if (!(valueElementType instanceof Class<?>) ||
                        !Classes.matches((Class<?>) valueElementType, targetIdProp.getElementClass())) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop.getName() +
                                    "\", the second generic type argument of resolver type \"" +
                                    resolverType.getName() +
                                    "\" must be \"" +
                                    List.class.getName() +
                                    "\" whose element type is \"" +
                                    targetIdProp.getElementClass() +
                                    "\" which is return type of \"" +
                                    targetIdProp +
                                    "\""
                    );
                }
            } else {
                if (!(valueType instanceof Class<?>) ||
                        !Classes.matches((Class<?>) valueType, targetIdProp.getElementClass())) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop.getName() +
                                    "\", the second generic type argument of resolver type \"" +
                                    resolverType.getName() +
                                    "\" must be \"" +
                                    targetIdProp.getElementClass() +
                                    "\" which is return type of \"" +
                                    targetIdProp +
                                    "\""
                    );
                }
            }
        } else if (!(valueType instanceof Class<?>) || !Classes.matches((Class<?>)valueType, prop.getElementClass())) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop.getName() +
                            "\", the second generic type argument of resolver type \"" +
                            resolverType.getName() +
                            "\" must be \"" +
                            prop.getElementClass() +
                            "\""
            );
        }

        if (resolver != null) {
            return resolver;
        }
        try {
            return provider.get((Class<TransientResolver<?,?>>) resolverType, sqlClient);
        } catch (Exception ex) {
            throw convertResolverConstructorError(prop, ex);
        }
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
}
