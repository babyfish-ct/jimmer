package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ScalarProviderUtils;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

abstract class AbstractValueGetter implements ValueGetter, GetterMetadata {

    private final JSqlClientImplementor sqlClient;

    final ImmutableProp valueProp;

    final Class<?> javaType;

    private final ScalarProvider<Object, Object> scalarProvider;

    private final String sqlTypeName;

    AbstractValueGetter(JSqlClientImplementor sqlClient, ImmutableProp valueProp) {
        this.sqlClient = sqlClient;
        this.valueProp = valueProp;
        if (valueProp == null) {
            this.javaType = null;
            this.scalarProvider = null;
            this.sqlTypeName = null;
        } else {
            this.javaType = valueProp.getReturnClass();
            this.scalarProvider = sqlClient.getScalarProvider(valueProp);
            Storage storage = valueProp.getStorage(sqlClient.getMetadataStrategy());
            if (storage instanceof SingleColumn) {
                this.sqlTypeName = ((SingleColumn) storage).getSqlType();
            } else {
                this.sqlTypeName = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    AbstractValueGetter(JSqlClientImplementor sqlClient, ExpressionImplementor<?> expression) {
        this.sqlClient = sqlClient;
        this.valueProp = null;
        this.javaType = expression.getType();
        this.scalarProvider = sqlClient.<Object, Object>getScalarProvider((Class<Object>) expression.getType());
        this.sqlTypeName = sqlClient
                .getMetadataStrategy()
                .getSqlTypeStrategy()
                .sqlType(expression.getType());
    }

    @Override
    public final Object get(Object value) {
        Object scalarValue = getRaw(value);
        if (valueProp != null) {
            return Variables.process(scalarValue, valueProp, sqlClient);
        }
        if (javaType != null) {
            return Variables.process(scalarValue, javaType, sqlClient);
        }
        return scalarValue;
    }

    protected abstract Object getRaw(Object value);

    static List<ValueGetter> createValueGetters(
            JSqlClientImplementor sqlClient,
            ImmutableProp prop,
            Object value
    ) {
        return createValueGetters(
                sqlClient,
                null,
                false,
                Collections.singletonList(prop),
                value
        );
    }

    static List<ValueGetter> createValueGetters(
            JSqlClientImplementor sqlClient,
            Table<?> table,
            boolean rawId,
            List<ImmutableProp> props,
            Object value
    ) {
        boolean inverse = false;
        if (table != null) {
            if (table instanceof TableProxy<?>) {
                inverse = ((TableProxy<?>) table).__isInverse();
            } else {
                inverse = ((TableImplementor<?>) table).isInverse();
            }
        }
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        ColumnDefinition definition = null;
        ImmutableProp originalRootProp = props.get(0);
        if (isDirect(sqlClient, table, props, rawId)) {
            if (originalRootProp.isColumnDefinition()) {
                definition = originalRootProp.getStorage(strategy);
            } else if (props.size() > 1) {
                ImmutableProp mappedBy = originalRootProp.getMappedBy();
                if (mappedBy != null) {
                    Storage storage = mappedBy.getStorage(strategy);
                    if (storage instanceof MiddleTable) {
                        if (inverse) {
                            definition = ((MiddleTable) storage).getTargetColumnDefinition();
                        } else {
                            definition = ((MiddleTable) storage).getColumnDefinition();
                        }
                    } else if (inverse) {
                        definition = (ColumnDefinition) storage;
                    }
                } else if (originalRootProp.isMiddleTableDefinition()) {
                    MiddleTable middleTable = originalRootProp.getStorage(strategy);
                    if (inverse) {
                        definition = middleTable.getColumnDefinition();
                    } else {
                        definition = middleTable.getTargetColumnDefinition();
                    }
                }
                if (definition != null) {
                    props = props.subList(1, props.size());
                }
            }
        }
        if (definition == null) {
            if (props.size() > 1 && props.get(1).isColumnDefinition()) {
                definition = props.get(1).getStorage(strategy);
                props = props.subList(1, props.size());
                rawId = false;
            } else {
                return Collections.singletonList(new TransientValueGetter(sqlClient, props));
            }
        }
        ImmutableProp rootProp = props.get(0);
        List<ImmutableProp> restProps;
        if (props.size() == 1) {
            restProps = Collections.emptyList();
        } else if (rootProp.isAssociation(TargetLevel.ENTITY)) {
            restProps = props.subList(2, props.size());
        } else {
            restProps = props.subList(1, props.size());
        }
        if (definition instanceof MultipleJoinColumns) {
            MultipleJoinColumns joinColumns = (MultipleJoinColumns) definition;
            EmbeddedColumns targetIdColumns =
                    inverse ?
                            originalRootProp.getDeclaringType().getIdProp().getStorage(strategy) :
                            originalRootProp.getTargetType().getIdProp().getStorage(strategy);
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
                                    sqlClient,
                                    deeperProps,
                                    table,
                                    true,
                                    rawId,
                                    embeddedProps.get(embeddedProps.size() - 1),
                                    columnName
                            )
                    );
                }
            }
            return getters;
        }
        if (definition instanceof EmbeddedColumns) {
            EmbeddedColumns embeddedColumns = (EmbeddedColumns) definition;
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
                                    sqlClient,
                                    deeperProps,
                                    table,
                                    rawId,
                                    false,
                                    embeddedProps.get(embeddedProps.size() - 1),
                                    columnName
                            )
                    );
                }
            }
            return getters;
        }
        return Collections.singletonList(
                new SimpleValueGetter(
                        sqlClient,
                        rootProp.isReference(TargetLevel.ENTITY) ?
                                rootProp.getTargetType().getIdProp() :
                                rootProp,
                        table,
                        rawId,
                        definition.name(0)
                )
        );
    }

    private static boolean isDirect(
            JSqlClientImplementor sqlClient,
            Table<?> table,
            List<ImmutableProp> props,
            boolean rawId
    ) {
        if (table == null) {
            return true;
        }
        ImmutableProp rootProp = props.get(0);
        if (!rootProp.isAssociation(TargetLevel.ENTITY)) {
            return true;
        }
        if (props.size() > 1 && !props.get(1).isId()) {
            return false;
        }
        return rawId || TableUtils.isRawIdAllowed(table, sqlClient);
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
        ImmutableProp prop = getValueProp();
        return prop != null && prop.getDefaultValueRef() != null;
    }

    @Override
    public final Object getDefaultValue() {
        ImmutableProp vp = getValueProp();
        if (vp == null) {
            return null;
        }
        Ref<Object> ref = vp.getDefaultValueRef();
        if (ref == null) {
            return null;
        }
        Object value = ref.getValue();
        Object suppliedValue = value instanceof Supplier<?> ? ((Supplier<?>) value).get() : value;
        return Variables.process(suppliedValue, vp, sqlClient);
    }

    @Override
    public Class<?> getSqlType() {
        return scalarProvider != null ?
                ScalarProviderUtils.getSqlType(scalarProvider, sqlClient.getDialect()) :
                getValueProp().getReturnClass();
    }

    @Override
    public String getSqlTypeName() {
        return sqlTypeName;
    }
}
