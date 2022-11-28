package org.babyfish.jimmer.impl.converter;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImmutableConverterBuilderImpl<T, Static> implements ImmutableConverter.Builder<T, Static> {

    private final ImmutableType immutableType;

    private final Class<Static> staticType;

    private BiConsumer<Draft, Static> draftModifier;

    private Map<ImmutableProp, ImmutableConverterImpl.Mapping> mappingMap = new HashMap<>();

    private boolean autoMapOtherScalars;

    public ImmutableConverterBuilderImpl(Class<T> immutableType, Class<Static> staticType) {
        this.immutableType = ImmutableType.get(immutableType);
        this.staticType = staticType;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> map(
            ImmutableProp prop,
            String staticPropName,
            Function<Object, Object> valueConverter
    ) {
        validateProp(prop);
        mapImpl(prop, staticPropName, valueConverter, false);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ImmutableConverter.Builder<T, Static> mapList(ImmutableProp prop, String staticPropName, Function<Object, Object> elementConverter) {
        validateProp(prop);
        if (!prop.isReferenceList(TargetLevel.OBJECT) && !prop.isScalarList()) {
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" is not list property"
            );
        }
        return map(prop, staticPropName, value ->
                ((List<Object>)value).stream()
                        .map(it -> it != null ? elementConverter.apply(it) : null)
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
    public ImmutableConverter.Builder<T, Static> autoMapOtherScalars() {
        autoMapOtherScalars = true;
        return this;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> setDraftModifier(BiConsumer<Draft, Static> draftModifier) {
        this.draftModifier = draftModifier;
        return this;
    }

    @Override
    public ImmutableConverter<T, Static> build() {
        if (autoMapOtherScalars) {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (!prop.isAssociation(TargetLevel.OBJECT) && !mappingMap.containsKey(prop)) {
                    mapImpl(prop, prop.getName(), null, true);
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
            ImmutableProp prop,
            String staticPropName,
            Function<Object, Object> valueConverter,
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
        if (method == null) {
            throw new IllegalArgumentException(
                    (autoMapping ?
                            "Cannot automatically map the property \"" + prop + "\"" :
                            "Illegal static property name: \"" + staticPropName + '"'
                    ) +
                            ", the following methods cannot be found in static type \"" +
                            staticType.getName() +
                            "\": " +
                            methodNames.stream().map(it -> it + "()").collect(Collectors.joining(", "))
            );
        }
        mappingMap.put(prop, ImmutableConverterImpl.Mapping.create(prop, method, valueConverter, autoMapping));
    }
}
