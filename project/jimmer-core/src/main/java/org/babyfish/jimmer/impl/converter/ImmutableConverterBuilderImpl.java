package org.babyfish.jimmer.impl.converter;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ImmutableConverterBuilderImpl<T, Static> implements ImmutableConverter.Builder<T, Static> {

    private static final int FULL_AUTO_MAPPING = 1;

    private static final int PARTIAL_AUTO_MAPPING = 2;

    private final ImmutableType immutableType;

    private final Class<Static> staticType;

    private BiConsumer<Draft, Static> draftModifier;

    private Map<ImmutableProp, ImmutableConverterImpl.Mapping> mappingMap = new HashMap<>();

    private int autoMapOtherScalars;

    public ImmutableConverterBuilderImpl(Class<T> immutableType, Class<Static> staticType) {
        this.immutableType = ImmutableType.get(immutableType);
        this.staticType = staticType;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> mapIf(
            Predicate<Static> cond,
            ImmutableProp prop,
            String staticPropName,
            ImmutableConverter.ValueConverter valueConverter
    ) {
        validateProp(prop);
        mapImpl(cond, prop, staticPropName, valueConverter, false);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableConverter.Builder<T, Static> mapListIf(
            Predicate<Static> cond,
            ImmutableProp prop,
            String staticPropName,
            ImmutableConverter.ValueConverter elementConverter
    ) {
        validateProp(prop);
        if (!prop.isReferenceList(TargetLevel.OBJECT) && !prop.isScalarList()) {
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" is not list property"
            );
        }
        return mapIf(cond, prop, staticPropName, value ->
                ((List<Object>)value).stream()
                        .map(it -> {
                            if (elementConverter == null) {
                                return it;
                            }
                            return it == null ?
                                    elementConverter.defaultValue() :
                                    elementConverter.convert(it);
                        })
                        .collect(Collectors.toList())
        );
    }

    @Override
    public ImmutableConverter.Builder<T, Static> unmap(ImmutableProp ... props) {
        for (ImmutableProp prop : props) {
            validateProp(prop);
            mappingMap.put(prop, null);
        }
        return this;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> autoMapOtherScalars(boolean partial) {
        autoMapOtherScalars = partial ? PARTIAL_AUTO_MAPPING : FULL_AUTO_MAPPING;
        return this;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> setDraftModifier(BiConsumer<Draft, Static> draftModifier) {
        this.draftModifier = draftModifier;
        return this;
    }

    @Override
    public ImmutableConverter<T, Static> build() {
        if (autoMapOtherScalars != 0) {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (!prop.isAssociation(TargetLevel.OBJECT) && !mappingMap.containsKey(prop)) {
                    mapImpl(null, prop, prop.getName(), null, true);
                }
            }
        }
        return new ImmutableConverterImpl<>(
                immutableType,
                staticType,
                new ArrayList<>(mappingMap.values()),
                draftModifier
        );
    }

    private void validateProp(ImmutableProp prop) {
        ImmutableType declaringType = prop.getDeclaringType();
        for (ImmutableType type = immutableType; type != null; type = type.getSuperType()) {
            if (declaringType == type) {
                return;
            }
        }
        throw new IllegalArgumentException(
                "\"" +
                        prop +
                        "\" is not property of \"" +
                        immutableType +
                        "\""
        );
    }

    private void mapImpl(
            Predicate<Static> cond,
            ImmutableProp prop,
            String staticPropName,
            ImmutableConverter.ValueConverter valueConverter,
            boolean autoMapping
    ) {
        List<String> methodNames = new ArrayList<>();
        String suffix = Character.toUpperCase(staticPropName.charAt(0)) + staticPropName.substring(1);
        if (prop.getElementClass() == boolean.class) {
            methodNames.add("is" + suffix);
        }
        methodNames.add("get" + suffix);
        methodNames.add(staticPropName);
        Method method = null;
        for (String methodName : methodNames) {
            try {
                method = staticType.getMethod(methodName);
                break;
            } catch (NoSuchMethodException ex) {
                // Do nothing
            }
        }
        if (method == null || Modifier.isStatic(method.getModifiers())) {
            if (autoMapping && autoMapOtherScalars == PARTIAL_AUTO_MAPPING) {
                return;
            }
            throw new IllegalArgumentException(
                    (autoMapping ?
                            "Cannot automatically map the property \"" + prop + "\"" :
                            "Illegal static property name: \"" + staticPropName + '"'
                    ) +
                            ", the following non-static methods cannot be found in static type \"" +
                            staticType.getName() +
                            "\": " +
                            methodNames.stream().map(it -> it + "()").collect(Collectors.joining(", "))
            );
        }
        mappingMap.put(
                prop,
                ImmutableConverterImpl.Mapping.create(
                        cond,
                        prop,
                        method,
                        valueConverter,
                        autoMapping
                )
        );
    }
}
