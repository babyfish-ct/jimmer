package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.lang.Generics;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.KeyMatcher;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

class DraftInterceptorManager {

    private final Map<ImmutableType, List<DraftInterceptor<?, ?>>> interceptorMap;

    private final TypeCache<DraftInterceptor<?, ?>> cache =
            new TypeCache<>(this::create, true);

    DraftInterceptorManager(Collection<DraftInterceptor<?, ?>> interceptors) {
        Map<ImmutableType, List<DraftInterceptor<?, ?>>> interceptorMap = new HashMap<>();
        for (DraftInterceptor<?, ?> interceptor : interceptors) {
            if (interceptor != null) {
                Type[] argTypes = Generics
                        .getTypeArguments(
                                interceptor.getClass(),
                                DraftInterceptor.class
                        );
                if (argTypes.length == 0) {
                    throw new IllegalArgumentException(
                            "Illegal draft interceptor type \"" +
                                    interceptor.getClass().getName() +
                                    "\", it extends \"" +
                                   DraftInterceptor.class.getName() +
                                    "\" but the generic type arguments are not specified"
                    );
                }
                Type entityType = argTypes[0];
                Type draftType = argTypes[1];
                if (!(entityType instanceof Class<?>) || 
                        !((Class<?>) entityType).isInterface() || (
                                ((Class<?>) entityType).getAnnotation(Entity.class) == null &&
                                        ((Class<?>) entityType).getAnnotation(MappedSuperclass.class) == null
                        )
                ) {
                    throw new IllegalArgumentException(
                            "Illegal draft interceptor type \"" +
                                    interceptor.getClass().getName() +
                                    "\", it extends \"" +
                                    DraftInterceptor.class.getName() +
                                    "\" but its entity type \"" +
                                    entityType +
                                    "\" is not interface type decorated by \"@" +
                                    Entity.class.getName() +
                                    "\" or \"@" +
                                    MappedSuperclass.class.getName() +
                                    "\""
                    );
                }
                if (!(draftType instanceof Class<?>) || 
                        !((Class<?>) draftType).isInterface() ||
                        !Draft.class.isAssignableFrom((Class<?>) draftType) || 
                        !((Class<?>)entityType).isAssignableFrom((Class<?>) draftType)) {
                    throw new IllegalArgumentException(
                            "Illegal draft interceptor type \"" +
                                    interceptor.getClass().getName() +
                                    "\", it extends \"" +
                                    DraftInterceptor.class.getName() +
                                    "\" but its draft type \"" +
                                    ((Class<?>)draftType).getName() +
                                    "\" is not interface type which is draft interface of the entity type \"" +
                                    ((Class<?>)entityType).getName() +
                                    "\""
                    );
                }
                ImmutableType immutableType = ImmutableType.get((Class<?>) draftType);
                interceptorMap
                        .computeIfAbsent(immutableType, it -> new ArrayList<>())
                        .add(interceptor);
            }
        }
        this.interceptorMap = interceptorMap;
    }

    public DraftInterceptor<?, ?> get(ImmutableType type) {
        return cache.get(type);
    }

    @SuppressWarnings("unchecked")
    private DraftInterceptor<?, ?> create(ImmutableType type) {
        List<DraftInterceptor<?, ?>> interceptors = new ArrayList<>();
        Set<ImmutableType> allTypes = type.getAllTypes();
        for (ImmutableType t : allTypes) {
            List<DraftInterceptor<?, ?>> list = interceptorMap.get(t);
            if (list != null) {
                interceptors.addAll(list);
            }
        }
        if (interceptors.isEmpty()) {
            return null;
        }
        Set<TypedProp<?, ?>> dependencies = new LinkedHashSet<>();
        boolean ignoreIdOnly = false;
        for (DraftInterceptor<?, ?> interceptor : interceptors) {
            ignoreIdOnly |= interceptor.ignoreIdOnly();
            Collection<? extends TypedProp<?, ?>> typeProps = interceptor.dependencies();
            if (typeProps != null) {
                for (TypedProp<?, ?> typedProp : typeProps) {
                    ImmutableProp prop = typedProp.unwrap();
                    if (!prop.isColumnDefinition()) {
                        throw new IllegalArgumentException(
                                "Illegal draft interceptor type \"" +
                                        interceptor.getClass().getName() +
                                        "\", its \"dependencies\" contains the property \"" +
                                        prop +
                                        "\" which is not column definition"
                        );
                    }
                    if (!allTypes.contains(prop.getDeclaringType())) {
                        throw new IllegalArgumentException(
                                "Illegal draft interceptor type \"" +
                                        interceptor.getClass().getName() +
                                        "\", its \"dependencies\" contains the property \"" +
                                        prop +
                                        "\" which is not belong to the type \"" +
                                        type +
                                        "\""
                        );
                    }
                    if (!prop.isId()) {
                        dependencies.add(typedProp);
                    }
                }
            }
        }
        final boolean mergedIgnoreIdOnly = ignoreIdOnly;

        return new DraftInterceptor<ImmutableSpi, Draft>() {

            @Override
            public void beforeSave(@NotNull Draft draft, @Nullable ImmutableSpi original) {
                for (DraftInterceptor<?, ?> interceptor : interceptors) {
                    ((DraftInterceptor<ImmutableSpi, Draft>)interceptor).beforeSave(draft, original);
                }
            }

            @Override
            public void beforeSaveAll(@NotNull Collection<Item<ImmutableSpi, Draft>> items) {
                for (DraftInterceptor<?, ?> interceptor : interceptors) {
                    ((DraftInterceptor<ImmutableSpi, Draft>)interceptor).beforeSaveAll(items);
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public Collection<TypedProp<ImmutableSpi, ?>> dependencies() {
                return (Collection<TypedProp<ImmutableSpi, ?>>) (Collection<?>) dependencies;
            }

            @Override
            public boolean ignoreIdOnly() {
                return mergedIgnoreIdOnly;
            }

            @Override
            public boolean ignoreKeyOnly(@NotNull KeyMatcher.Group group) {
                for (DraftInterceptor<?, ?> interceptor : interceptors) {
                    if (interceptor.ignoreKeyOnly(group)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
