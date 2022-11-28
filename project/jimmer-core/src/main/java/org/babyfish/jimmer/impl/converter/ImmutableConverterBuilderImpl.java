package org.babyfish.jimmer.impl.converter;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class ImmutableConverterBuilderImpl<T, Static> implements ImmutableConverter.Builder<T, Static> {

    private static final int FULL_AUTO_MAPPING = 1;

    private static final int PARTIAL_AUTO_MAPPING = 2;

    private final ImmutableType immutableType;

    private final Class<Static> staticType;

    private BiConsumer<Draft, Static> draftModifier;

    private Map<ImmutableProp, ImmutableConverterImpl.Field> mappingMap = new HashMap<>();

    private int autoMapOtherScalars;

    public ImmutableConverterBuilderImpl(Class<T> immutableType, Class<Static> staticType) {
        this.immutableType = ImmutableType.get(immutableType);
        this.staticType = staticType;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> map(
            ImmutableProp prop,
            String staticPropName,
            Consumer<ImmutableConverter.Mapping<Static, ?, ?>> block
    ) {
        validateProp(prop);
        mapImpl(prop, staticPropName, block, false, false);
        return this;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> mapList(
            ImmutableProp prop,
            String staticPropName,
            Consumer<ImmutableConverter.ListMapping<Static, ?, ?>> block
    ) {
        validateProp(prop);
        if (!prop.isReferenceList(TargetLevel.OBJECT) && !prop.isScalarList()) {
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" is not list property"
            );
        }
        mapImpl(prop, staticPropName, block, true, false);
        return this;
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
                    mapImpl(
                            prop,
                            prop.getName(),
                            null,
                            prop.isScalarList() || prop.isReferenceList(TargetLevel.OBJECT),
                            true
                    );
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

    @SuppressWarnings("unchecked")
    private void mapImpl(
            ImmutableProp prop,
            String staticPropName,
            Consumer<?> mappingBuilderConsumer,
            boolean treatAsList,
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
        FieldBuilder builder = treatAsList ?
                new ListMappingImpl<>(prop, method, autoMapping) :
                new MappingImpl<>(prop, method, autoMapping);
        if (mappingBuilderConsumer != null) {
            ((Consumer<FieldBuilder>) mappingBuilderConsumer).accept(builder);
        }
        mappingMap.put(prop, builder.build());
    }
    
    private interface FieldBuilder {
        ImmutableConverterImpl.Field build();
    }

    private static class MappingImpl<Static, X, Y> implements ImmutableConverter.Mapping<Static, X, Y>, FieldBuilder {

        private final ImmutableProp prop;

        private final Method method;

        private final boolean autoMapping;

        private Predicate<?> cond;
        
        protected Function<?, ?> valueConverter;

        private Supplier<?> defaultValueSupplier;

        private MappingImpl(ImmutableProp prop, Method method, boolean autoMapping) {
            this.prop = prop;
            this.method = method;
            this.autoMapping = autoMapping;
        }

        @Override
        public ImmutableConverter.Mapping<Static, X, Y> useIf(Predicate<Static> cond) {
            this.cond = cond;
            return this;
        }

        @Override
        public ImmutableConverter.Mapping<Static, X, Y> valueConverter(Function<X, Y> valueConverter) {
            this.valueConverter = valueConverter;
            return this;
        }

        @Override
        public ImmutableConverter.Mapping<Static, X, Y> immutableValueConverter(ImmutableConverter<Y, X> valueConverter) {
            valueConverter(value -> valueConverter.convert((X)value));
            return this;
        }

        @Override
        public ImmutableConverter.Mapping<Static, X, Y> defaultValue(Y defaultValue) {
            this.defaultValueSupplier = () -> defaultValue;
            return this;
        }

        @Override
        public ImmutableConverter.Mapping<Static, X, Y> defaultValue(Supplier<Y> defaultValueSupplier) {
            this.defaultValueSupplier = defaultValueSupplier;
            return this;
        }

        @Override
        public ImmutableConverterImpl.Field build() {
            return ImmutableConverterImpl.Field.create(
                    cond,
                    prop,
                    method,
                    valueConverter,
                    defaultValueSupplier,
                    autoMapping
            );
        }
    }

    private static class ListMappingImpl<Static, X, Y> implements ImmutableConverter.ListMapping<Static, X, Y>, FieldBuilder {

        private final ImmutableProp prop;

        private final Method method;

        private final boolean autoMapping;

        private Predicate<?> cond;

        protected Function<?, ?> elementConverter;

        private Supplier<?> defaultElementSupplier;

        private ListMappingImpl(ImmutableProp prop, Method method, boolean autoMapping) {
            this.prop = prop;
            this.method = method;
            this.autoMapping = autoMapping;
        }

        @Override
        public ImmutableConverter.ListMapping<Static, X, Y> useIf(Predicate<Static> cond) {
            this.cond = cond;
            return this;
        }

        @Override
        public ImmutableConverter.ListMapping<Static, X, Y> elementConverter(Function<X, Y> elementConverter) {
            this.elementConverter = new ListConverter<>(elementConverter);
            return this;
        }

        @Override
        public ImmutableConverter.ListMapping<Static, X, Y> immutableValueConverter(ImmutableConverter<Y, X> elementConverter) {
            elementConverter(value -> elementConverter.convert((X)value));
            return this;
        }

        @Override
        public ImmutableConverter.ListMapping<Static, X, Y> defaultElement(Y defaultElement) {
            this.defaultElementSupplier = () -> defaultElement;
            return this;
        }

        @Override
        public ImmutableConverter.ListMapping<Static, X, Y> defaultElement(Supplier<Y> defaultElementSupplier) {
            this.defaultElementSupplier = defaultElementSupplier;
            return this;
        }

        @Override
        public ImmutableConverterImpl.Field build() {
            return ImmutableConverterImpl.Field.create(
                    cond,
                    prop,
                    method,
                    elementConverter,
                    defaultElementSupplier,
                    autoMapping
            );
        }

        private class ListConverter<X, Y> implements Function<List<X>, List<Y>> {

            private final Function<X, Y> elementConverter;

            private ListConverter(Function<X, Y> elementConverter) {
                this.elementConverter = elementConverter;
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<Y> apply(List<X> list) {
                List<Y> newList = new ArrayList<>(list.size());
                for (X x : list) {
                    Y y;
                    if (x == null) {
                        if (defaultElementSupplier == null) {
                            y = null;
                        } else {
                            y = (Y) defaultElementSupplier.get();
                        }
                    } else {
                        if (elementConverter == null) {
                            y = (Y)x;
                        } else {
                            y = elementConverter.apply(x);
                        }
                    }
                    newList.add(y);
                }
                return newList;
            }
        }
    }
}
