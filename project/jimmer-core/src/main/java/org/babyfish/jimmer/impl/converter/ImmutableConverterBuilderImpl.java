package org.babyfish.jimmer.impl.converter;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.impl.util.PropName;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class ImmutableConverterBuilderImpl<T, Static> implements ImmutableConverter.Builder<T, Static> {

    private final ImmutableType immutableType;

    private final Class<Static> staticType;

    private final Map<String, StaticProp> staticPropMap;

    private BiConsumer<Draft, Static> draftModifier;

    private Map<ImmutableProp, ImmutableConverterImpl.Field> mappingMap = new HashMap<>();

    public ImmutableConverterBuilderImpl(Class<T> immutableType, Class<Static> staticType, boolean byField) {
        this.immutableType = ImmutableType.get(immutableType);
        this.staticType = staticType;
        this.staticPropMap = staticProps(staticType, byField);
        if (staticPropMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot map the static type \"" +
                            staticType.getName() +
                            "\" by " +
                            (byField ? "FIELDS" : "METHODS") +
                            ", not static properties has been found"
            );
        }
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
    public ImmutableConverter.Builder<T, Static> unmapStaticProps(Collection<String> staticPropNames) {
        for (String staticPropName : staticPropNames) {
            staticProp(staticPropName, false).mapped = true;
        }
        return this;
    }

    @Override
    public ImmutableConverter.Builder<T, Static> setDraftModifier(BiConsumer<Draft, Static> draftModifier) {
        this.draftModifier = draftModifier;
        return this;
    }

    @Override
    public ImmutableConverter<T, Static> build() {
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
        List<StaticProp> unmappedStaticProps = staticPropMap
                .values()
                .stream()
                .filter(it -> !it.mapped)
                .collect(Collectors.toList());
        if (!unmappedStaticProps.isEmpty()) {
            throw new IllegalArgumentException(
                    unmappedStaticProps + " has not not been mapped"
            );
        }
        return new ImmutableConverterImpl<>(
                immutableType,
                staticType,
                mappingMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList()),
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
        StaticProp staticProp = staticProp(staticPropName, autoMapping);
        if (staticProp == null) {
            return;
        }
        staticProp.mapped = true;
        FieldBuilder builder = treatAsList ?
                new ListMappingImpl<>(prop, staticProp.method, staticProp.field, autoMapping) :
                new MappingImpl<>(prop, staticProp.method, staticProp.field, autoMapping);
        if (mappingBuilderConsumer != null) {
            ((Consumer<FieldBuilder>) mappingBuilderConsumer).accept(builder);
        }
        mappingMap.put(prop, builder.build());
    }

    private StaticProp staticProp(String staticPropName, boolean nullable) {
        StaticProp staticProp = staticPropMap.get(staticPropName);
        if (nullable || staticProp != null) {
            return staticProp;
        }
        throw new IllegalArgumentException(
                "Illegal static property name \"" +
                        staticPropName +
                        "\", available choices are " +
                        staticPropMap.keySet()
        );
    }

    private static Map<String, StaticProp> staticProps(Class<?> staticType, boolean byField) {
        Map<String, StaticProp> possiblePropMap = new HashMap<>();
        possibleStaticProps(staticType, byField, possiblePropMap);
        Map<String, StaticProp> map = new TreeMap<>();
        for (Map.Entry<String, StaticProp> e : possiblePropMap.entrySet()) {
            if (e.getValue().use()) {
                map.put(e.getKey(), e.getValue());
            }
        }
        return map;
    }

    private static void possibleStaticProps(Class<?> staticType, boolean byField, Map<String, StaticProp> map) {
        if (staticType == null || staticType == Object.class) {
            return;
        }
        if (!byField) {
            for (Method method : staticType.getDeclaredMethods()) {
                PropName propName = PropName.fromBeanGetter(method);
                if (propName != null) {
                    StaticProp staticProp = map.get(propName.getText());
                    if (staticProp == null) {
                        staticProp = new StaticProp(propName.getText(), propName.isRecordStyle());
                        staticProp.method = method;
                        map.put(propName.getText(), staticProp);
                    } else if (staticProp.mayBe) {
                        staticProp.method = method;
                        staticProp.mayBe = propName.isRecordStyle();
                    }
                }
            }
        }
        for (Field field : staticType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            StaticProp staticProp = map.get(field.getName());
            if (staticProp == null) {
                staticProp = new StaticProp(field.getName(), !byField);
                staticProp.field = field;
                map.put(field.getName(), staticProp);
            } else if (staticProp.mayBe) {
                staticProp.field = field;
                staticProp.mayBe = !byField;
            }
        }
        possibleStaticProps(staticType.getSuperclass(), byField, map);
        for (Class<?> itfType : staticType.getInterfaces()) {
            possibleStaticProps(itfType, byField, map);
        }
    }

    private static class StaticProp {
        final String name;
        boolean mayBe;
        Method method;
        Field field;
        boolean mapped;

        StaticProp(String name, boolean mayBe) {
            this.name = name;
            this.mayBe = mayBe;
        }

        boolean use() {
            if (!mayBe) {
                prepare();
                return true;
            }
            if (method != null && field != null) {
                Class<?> methodType = method.getReturnType();
                if (methodType == field.getType()) {
                    prepare();
                    return true;
                }
            }
            return false;
        }

        private void prepare() {
            if (method != null) {
                method.setAccessible(true);
            }
            if (field != null) {
                field.setAccessible(true);
            }
        }

        @Override
        public String toString() {
            return method != null ? method.toString() : field.toString();
        }
    }
    
    private interface FieldBuilder {
        ImmutableConverterImpl.Field build();
    }

    private static class MappingImpl<Static, X, Y> implements ImmutableConverter.Mapping<Static, X, Y>, FieldBuilder {

        private final ImmutableProp prop;

        private final Method method;

        private final Field field;

        private final boolean autoMapping;

        private Predicate<?> cond;
        
        protected Function<?, ?> valueConverter;

        private Supplier<?> defaultValueSupplier;

        private MappingImpl(ImmutableProp prop, Method method, Field field, boolean autoMapping) {
            this.prop = prop;
            this.method = method;
            this.field = field;
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
                    field,
                    valueConverter,
                    defaultValueSupplier,
                    autoMapping
            );
        }
    }

    private static class ListMappingImpl<Static, X, Y> implements ImmutableConverter.ListMapping<Static, X, Y>, FieldBuilder {

        private final ImmutableProp prop;

        private final Method method;

        private final Field field;

        private final boolean autoMapping;

        private Predicate<?> cond;

        protected Function<?, ?> elementConverter;

        private Supplier<?> defaultElementSupplier;

        private ListMappingImpl(ImmutableProp prop, Method method, Field field, boolean autoMapping) {
            this.prop = prop;
            this.method = method;
            this.field = field;
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
                    field,
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
