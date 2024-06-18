package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class AbstractValueGetter implements ValueGetter, GetterMetadata {

    private final ScalarProvider<Object, Object> scalarProvider;

    AbstractValueGetter(ScalarProvider<Object, Object> scalarProvider) {
        this.scalarProvider = scalarProvider;
    }

    @Override
    public final Object get(Object value) {
        Object scalarValue = scalar(value);
        if (scalarValue == null || scalarProvider == null) {
            return scalarValue;
        }
        try {
            return scalarProvider.toSql(scalarValue);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot convert the value \"" +
                            scalarValue +
                            "\" to sql value by the scalar provider \"" +
                            scalarProvider +
                            "\""
            );
        }
    }

    protected abstract Object scalar(Object value);

    static List<ValueGetter> createValueGetters(
            JSqlClientImplementor sqlClient,
            ImmutableProp prop,
            Object value
    ) {
        return createValueGetters(
                sqlClient,
                Collections.singletonList(prop),
                value
        );
    }

    static List<ValueGetter> createValueGetters(
            JSqlClientImplementor sqlClient,
            List<ImmutableProp> props,
            Object value
    ) {
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        ImmutableProp rootProp = props.get(0);
        if (!rootProp.isColumnDefinition()) {
            return Collections.singletonList(
                    new SimpleValueGetter(
                            null,
                            rootProp,
                            null
                    )
            );
        }
        List<ImmutableProp> restProps;
        if (props.size() == 1) {
            restProps = Collections.emptyList();
        } else if (rootProp.isReference(TargetLevel.ENTITY)) {
            if (props.get(1) != rootProp.getTargetType().getIdProp()) {
                throw new IllegalArgumentException(
                        "The \"props[1]\" must be id property of the target of \"props[0]\""
                );
            }
            restProps = props.subList(2, props.size());
        } else {
            restProps = props.subList(1, props.size());
        }
        if (rootProp.isEmbedded(EmbeddedLevel.REFERENCE)) {
            MultipleJoinColumns joinColumns = rootProp.getStorage(strategy);
            EmbeddedColumns targetIdColumns = rootProp.getTargetType().getIdProp().getStorage(strategy);
            int size = joinColumns.size();
            List<ValueGetter> getters = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String columnName = joinColumns.name(i);
                String referencedColumnName = joinColumns.referencedName(i);
                List<ImmutableProp> embeddedProps = targetIdColumns.path(referencedColumnName);
                if (!startsWith(embeddedProps, restProps)) {
                    continue;
                }
                List<ImmutableProp> deeperProps = embeddedProps.subList(restProps.size(), embeddedProps.size());
                if (isLoaded(value, deeperProps)) {
                    getters.add(
                            new EmbeddedValueGetter(
                                    columnName,
                                    deeperProps,
                                    sqlClient.getScalarProvider(embeddedProps.get(embeddedProps.size() - 1))
                            )
                    );
                }
            }
            return getters;
        }
        if (rootProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            EmbeddedColumns embeddedColumns = rootProp.getStorage(strategy);
            int size = embeddedColumns.size();
            List<ValueGetter> getters = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                List<ImmutableProp> embeddedProps = embeddedColumns.path(i);
                if (!startsWith(embeddedProps, restProps)) {
                    continue;
                }
                String columnName = embeddedColumns.name(i);
                List<ImmutableProp> deeperProps = embeddedProps.subList(restProps.size(), embeddedProps.size());
                if (isLoaded(value, deeperProps)) {
                    getters.add(
                            new EmbeddedValueGetter(
                                    columnName,
                                    deeperProps,
                                    sqlClient.getScalarProvider(embeddedProps.get(embeddedProps.size() - 1))
                            )
                    );
                }
            }
            return getters;
        }
        SingleColumn singleColumn = rootProp.getStorage(strategy);
        return Collections.singletonList(
                new SimpleValueGetter(
                        singleColumn.getName(),
                        rootProp,
                        sqlClient.getScalarProvider(rootProp)
                )
        );
    }

    private static boolean startsWith(
            List<ImmutableProp> props,
            List<ImmutableProp> prefixProps
    ) {
        int size = prefixProps.size();
        if (props.size() < size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (prefixProps.get(i) != props.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLoaded(Object value, List<ImmutableProp> props) {
        for (ImmutableProp prop : props) {
            if (value == null) {
                return true;
            }
            ImmutableSpi spi = (ImmutableSpi) value;
            if (!spi.__isLoaded(prop.getId())) {
                return false;
            }
            value = spi.__get(prop.getId());
        }
        return true;
    }

    @Override
    public final GetterMetadata metadata() {
        return this;
    }

    @Override
    public final boolean isJson() {
        return scalarProvider != null && scalarProvider.isJsonScalar();
    }

    @Override
    public boolean hasDefaultValue() {
        return getValueProp().getDefaultValueRef() != null;
    }

    @Override
    public final Object getDefaultValue() {
        ImmutableProp vp = getValueProp();
        Ref<Object> ref = vp.getDefaultValueRef();
        if (ref == null) {
            return null;
        }
        return ref.getValue();
    }

    @Override
    public Class<?> getSqlType() {
        if (scalarProvider != null) {
            return scalarProvider.getSqlType();
        }
        return getValueProp().getReturnClass();
    }
}
