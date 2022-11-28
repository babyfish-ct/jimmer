package org.babyfish.jimmer.sql;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.impl.util.StaticCache;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

class DraftInterceptorManager {

    private final Map<ImmutableType, List<DraftInterceptor<?>>> interceptorMap;

    private final StaticCache<ImmutableType, DraftInterceptor<?>> cache =
            new StaticCache<>(this::create, true);

    DraftInterceptorManager(Collection<DraftInterceptor<?>> interceptors) {
        Map<ImmutableType, List<DraftInterceptor<?>>> interceptorMap = new HashMap<>();
        for (DraftInterceptor<?> interceptor : interceptors) {
            if (interceptor != null) {
                Collection<Type> types = TypeUtils
                        .getTypeArguments(
                                interceptor.getClass(),
                                DraftInterceptor.class
                        )
                        .values();
                if (types.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Illegal draft interceptor type \"" +
                                    interceptor.getClass().getName() +
                                    "\", it extends \"DraftInterceptor\" but the generic type is not specified"
                    );
                }
                Type draftType = types.iterator().next();
                if (!(draftType instanceof Class<?>) || !((Class<?>) draftType).isInterface()) {
                    throw new IllegalArgumentException(
                            "Illegal draft interceptor type \"" +
                                    interceptor.getClass().getName() +
                                    "\", it extends \"DraftInterceptor\" but the generic type is not draft interface type"
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

    public DraftInterceptor<?> get(ImmutableType type) {
        return cache.get(type);
    }

    @SuppressWarnings("unchecked")
    private DraftInterceptor<?> create(ImmutableType type) {
        List<DraftInterceptor<?>> interceptors = new ArrayList<>();
        for (ImmutableType t = type; t != null; t = t.getSuperType()) {
            List<DraftInterceptor<?>> list = interceptorMap.get(t);
            if (list != null) {
                interceptors.addAll(list);
            }
        }
        if (interceptors.isEmpty()) {
            return null;
        }
        if (interceptors.size() == 1) {
            return interceptors.get(0);
        }
        return new DraftInterceptor<Draft>() {
            @Override
            public void beforeSave(@NotNull Draft draft, boolean isNew) {
                for (DraftInterceptor<?> interceptor : interceptors) {
                    ((DraftInterceptor<Draft>)interceptor).beforeSave(draft, isNew);
                }
            }
        };
    }
}
