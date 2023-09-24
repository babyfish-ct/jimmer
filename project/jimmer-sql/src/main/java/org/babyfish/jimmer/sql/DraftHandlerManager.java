package org.babyfish.jimmer.sql;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

class DraftHandlerManager {

    private final Map<ImmutableType, List<DraftHandler<?, ?>>> handlerMap;

    private final TypeCache<DraftHandler<?, ?>> cache =
            new TypeCache<>(this::create, true);

    DraftHandlerManager(Collection<DraftHandler<?, ?>> handlers) {
        Map<ImmutableType, List<DraftHandler<?, ?>>> handlerMap = new HashMap<>();
        for (DraftHandler<?, ?> handler : handlers) {
            if (handler != null) {
                DraftInterceptor<?> interceptor = DraftInterceptor.unwrap(handler);
                Object derivedObject = interceptor != null ? interceptor : handler;
                Class<?> baseType = interceptor != null ? DraftInterceptor.class : DraftHandler.class;
                Collection<Type> types = TypeUtils
                        .getTypeArguments(
                                derivedObject.getClass(),
                                baseType
                        )
                        .values();
                if (types.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Illegal type \"" +
                                    derivedObject.getClass().getName() +
                                    "\", it extends \"" +
                                   baseType.getName() +
                                    "\" but the generic type is not specified"
                    );
                }
                Type draftType = types.iterator().next();
                if (!(draftType instanceof Class<?>) || !((Class<?>) draftType).isInterface()) {
                    throw new IllegalArgumentException(
                            "Illegal draft type \"" +
                                    derivedObject.getClass().getName() +
                                    "\", it extends \"" +
                                    baseType.getName() +
                                    "\" but the generic type is not draft interface type"
                    );
                }
                ImmutableType immutableType = ImmutableType.get((Class<?>) draftType);
                handlerMap
                        .computeIfAbsent(immutableType, it -> new ArrayList<>())
                        .add(handler);
            }
        }
        this.handlerMap = handlerMap;
    }

    public DraftHandler<?, ?> get(ImmutableType type) {
        return cache.get(type);
    }

    @SuppressWarnings("unchecked")
    private DraftHandler<?, ?> create(ImmutableType type) {
        List<DraftHandler<?, ?>> handlers = new ArrayList<>();
        Set<ImmutableType> allTypes = type.getAllTypes();
        for (ImmutableType t : allTypes) {
            List<DraftHandler<?, ?>> list = handlerMap.get(t);
            if (list != null) {
                handlers.addAll(list);
            }
        }
        if (handlers.isEmpty()) {
            return null;
        }
        Set<ImmutableProp> dependencies = new LinkedHashSet<>();
        for (DraftHandler<?, ?> handler : handlers) {
            for (ImmutableProp prop : handler.dependencies()) {
                if (!prop.isColumnDefinition()) {
                    throw new IllegalArgumentException(
                            "Illegal draft handler type \"" +
                                    handler.getClass().getName() +
                                    "\", its \"dependencies\" contains the property \"" +
                                    prop +
                                    "\" which is not column definition"
                    );
                }
                if (!allTypes.contains(prop.getDeclaringType())) {
                    throw new IllegalArgumentException(
                            "Illegal draft handler type \"" +
                                    handler.getClass().getName() +
                                    "\", its \"dependencies\" contains the property \"" +
                                    prop +
                                    "\" which is not belong to the type \"" +
                                    type +
                                    "\""
                    );
                }
                if (!prop.isId()) {
                    dependencies.add(prop);
                }
            }
        }

        return new DraftHandler<Draft, ImmutableSpi>() {
            @Override
            public void beforeSave(@NotNull Draft draft, @Nullable ImmutableSpi original) {
                for (DraftHandler<?, ?> handler : handlers) {
                    ((DraftHandler<Draft, ImmutableSpi>)handler).beforeSave(draft, original);
                }
            }

            @Override
            public Collection<ImmutableProp> dependencies() {
                return dependencies;
            }
        };
    }
}
