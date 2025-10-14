package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.impl.util.GenericValidator;
import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.lang.Generics;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.cache.spi.PropCacheInvalidators;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.di.AopProxyProvider;
import org.babyfish.jimmer.sql.di.StrategyProvider;
import org.babyfish.jimmer.sql.di.TransientResolverProvider;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class TransientResolverManager {

    private final TransientResolverProvider transientResolverProvider;

    private final AopProxyProvider aopProxyProvider;

    private JSqlClient sqlClient;

    private final PropCache<TransientResolver<?, ?>> resolverCache =
            new PropCache<>(this::createResolver, true);

    TransientResolverManager(TransientResolverProvider transientResolverProvider, AopProxyProvider aopProxyProvider) {
        this.transientResolverProvider = transientResolverProvider;
        this.aopProxyProvider = aopProxyProvider;
    }

    void initialize(JSqlClient sqlClient) {
        if (this.sqlClient != null) {
            throw new IllegalStateException("The current object has been initialized");
        }
        this.sqlClient = sqlClient;
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

    public StrategyProvider<TransientResolver<?,?>> getTransientResolverProvider() {
        return transientResolverProvider;
    }

    private TransientResolver<?, ?> createResolver(ImmutableProp prop) {
        TransientResolver<?, ?> resolver = createResolver0(prop);
        if (resolver != null) {
            Cache<Object, ?> cache = sqlClient().getCaches().getPropertyCache(prop);
            if (cache != null &&
                    PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                            resolver,
                            EntityEvent.class,
                            aopProxyProvider
                    )
            ) {
                sqlClient().getTriggers().addEntityListener(e -> {
                    Collection<?> ids = resolver.getAffectedSourceIds(e);
                    if (ids != null && !ids.isEmpty()) {
                        List<Object> nonNullIds = new ArrayList<>(ids.size());
                        for (Object id : ids) {
                            if (id != null) {
                                nonNullIds.add(id);
                            }
                        }
                        if (!nonNullIds.isEmpty()) {
                            cache.deleteAll(nonNullIds);
                        }
                    }
                });
            }
            if (cache != null &&
                    PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                            resolver,
                            AssociationEvent.class,
                            aopProxyProvider
                    )
            ) {
                sqlClient().getTriggers().addAssociationListener(e -> {
                    Collection<?> ids = resolver.getAffectedSourceIds(e);
                    if (ids != null && !ids.isEmpty()) {
                        List<Object> nonNullIds = new ArrayList<>(ids.size());
                        for (Object id : ids) {
                            if (id != null) {
                                nonNullIds.add(id);
                            }
                        }
                        if (!nonNullIds.isEmpty()) {
                            cache.deleteAll(nonNullIds);
                        }
                    }
                });
            }
        }
        return resolver;
    }

    @SuppressWarnings("unchecked")
    private TransientResolver<?, ?> createResolver0(ImmutableProp prop) {
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
                resolver = transientResolverProvider.get(resolverRef, sqlClient);
            } catch (Exception ex) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the \"" +
                                transientResolverProvider.getClass().getName() +
                                ".get(String)\" throws exception",
                        ex
                );
            }
            if (resolver == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the \"" +
                                transientResolverProvider.getClass().getName() +
                                ".get(String)\" returns null"
                );
            }
            resolverType = resolver.getClass();
        }
        Type expectedType;
        if (prop.isReferenceList(TargetLevel.ENTITY)) {
            expectedType = Generics.makeParameterizedType(
                    List.class,
                    Generics.makeWildcardType(
                            new Type[] { Classes.boxTypeOf(prop.getTargetType().getIdProp().getReturnClass()) },
                            null
                    )
            );
        } else if (prop.isReference(TargetLevel.ENTITY)) {
            expectedType = Classes.boxTypeOf(prop.getTargetType().getIdProp().getReturnClass());
        } else {
            expectedType = prop.getGenericType();
        }
        new GenericValidator(prop, Transient.class, resolverType, TransientResolver.class)
                .expect(0, prop.getDeclaringType().getIdProp().getGenericType())
                .expect(1, expectedType, true)
                .validate();
        if (resolver != null) {
            return resolver;
        }
        try {
            return transientResolverProvider.get((Class<TransientResolver<?,?>>) resolverType, sqlClient);
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

    private JSqlClient sqlClient() {
        JSqlClient sqlClient = this.sqlClient;
        if (sqlClient == null) {
            throw new IllegalStateException(
                    "The transient resolver manager is not ready because the initialization of sqlClient is 'MANUAL' " +
                            "but the sqlClient is not initialized"
            );
        }
        return sqlClient;
    }
}
