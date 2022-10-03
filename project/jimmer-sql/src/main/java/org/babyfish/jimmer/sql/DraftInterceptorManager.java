package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.util.StaticCache;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DraftInterceptorManager {

    private final Map<ImmutableType, List<DraftInterceptor<?>>> map;

    private final StaticCache<ImmutableType, DraftInterceptor<?>> cache =
            new StaticCache<>(this::create, true);

    DraftInterceptorManager(Map<ImmutableType, List<DraftInterceptor<?>>> map) {
        this.map = map.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        it -> new ArrayList<>(it.getValue())
                )
        );
    }

    public DraftInterceptor<?> get(ImmutableType type) {
        return cache.get(type);
    }

    @SuppressWarnings("unchecked")
    private DraftInterceptor<?> create(ImmutableType type) {
        List<DraftInterceptor<?>> interceptors = new ArrayList<>();
        for (ImmutableType t = type; t != null; t = t.getSuperType()) {
            List<DraftInterceptor<?>> list = map.get(t);
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
