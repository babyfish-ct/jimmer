package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class AbstractValueGetter implements ValueGetter {

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
        if (!prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "Cannot create getters for property \"" +
                            prop +
                            "\" because it is column definition property"
            );
        }
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        if (prop.isEmbedded(EmbeddedLevel.REFERENCE)) {
            MultipleJoinColumns joinColumns = prop.getStorage(strategy);
            EmbeddedColumns targetIdColumns = prop.getTargetType().getIdProp().getStorage(strategy);
            int size = joinColumns.size();
            List<ValueGetter> getters = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String columnName = joinColumns.name(i);
                String referencedColumnName = joinColumns.referencedName(i);
                List<ImmutableProp> props = targetIdColumns.path(referencedColumnName);
                if (isLoaded(value, props)) {
                    getters.add(
                            new EmbeddedValueGetter(
                                    sqlClient,
                                    columnName,
                                    props,
                                    sqlClient.getScalarProvider(props.get(props.size() - 1))
                            )
                    );
                }
            }
            return getters;
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            EmbeddedColumns embeddedColumns = prop.getStorage(strategy);
            int size = embeddedColumns.size();
            List<ValueGetter> getters = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String columnName = embeddedColumns.name(i);
                List<ImmutableProp> props = embeddedColumns.path(i);
                if (isLoaded(value, props)) {
                    getters.add(
                            new EmbeddedValueGetter(
                                    sqlClient,
                                    columnName,
                                    props,
                                    sqlClient.getScalarProvider(props.get(props.size() - 1))
                            )
                    );
                }
            }
            return getters;
        }
        SingleColumn singleColumn = prop.getStorage(strategy);
        return Collections.singletonList(
                new SimpleValueGetter(
                        singleColumn.getName(),
                        sqlClient.getScalarProvider(prop)
                )
        );
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
}
