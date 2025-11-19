package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class ReferencePropertyGetter extends AbstractPropertyGetter implements GetterMetadata {

    private final ImmutableProp targetIdProp;

    ReferencePropertyGetter(@Nullable String alias, ImmutableProp prop, ValueGetter valueGetter) {
        super(alias, prop, valueGetter);
        this.targetIdProp = prop.getTargetType().getIdProp();
    }

    @Override
    public Object get(Object value) {
        ImmutableSpi spi = (ImmutableSpi) value;
        ImmutableSpi reference = (ImmutableSpi) spi.__get(prop.getId());
        if (reference == null) {
            return null;
        }
        return valueGetter.get(reference.__get(targetIdProp.getId()));
    }

    @Override
    public GetterMetadata metadata() {
        return this;
    }

    @Override
    String toStringPrefix() {
        return prop.getName() + '.' + targetIdProp.getName();
    }

    @Override
    public ImmutableProp getValueProp() {
        return targetIdProp;
    }

    @Override
    public Class<?> getSqlType() {
        return targetIdProp.getReturnClass();
    }

    @Override
    public @Nullable String getColumnName() {
        return valueGetter.metadata().getColumnName();
    }

    @Override
    public boolean isForeignKey() {
        return true;
    }

    @Override
    public boolean isNullable() {
        return valueGetter.metadata().isNullable();
    }

    @Override
    public boolean isJson() {
        return false;
    }

    @Override
    public boolean hasDefaultValue() {
        return false;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String getSqlTypeName() {
        return valueGetter.metadata().getSqlTypeName();
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        valueGetter.metadata().renderTo(builder);
    }

    static List<PropertyGetter> getters(@Nullable String alias, ImmutableProp prop, List<ValueGetter> valueGetters) {
        List<PropertyGetter> propertyGetters = new ArrayList<>(valueGetters.size());
        for (ValueGetter valueGetter : valueGetters) {
            propertyGetters.add(new ReferencePropertyGetter(alias, prop, valueGetter));
        }
        return propertyGetters;
    }
}
