package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;

class AliasValueGetter implements ValueGetter, GetterMetadata {

    private final String alias;

    private final ValueGetter raw;

    AliasValueGetter(String alias, ValueGetter raw) {
        this.alias = alias;
        this.raw = raw;
    }

    @Override
    public Object get(Object value) {
        return raw.get(value);
    }

    @Override
    public GetterMetadata metadata() {
        return this;
    }

    @Override
    public ImmutableProp getValueProp() {
        return null;
    }

    @Override
    public @Nullable String getColumnName() {
        return raw.metadata().getColumnName();
    }

    @Override
    public boolean isNullable() {
        return raw.metadata().isNullable();
    }

    @Override
    public boolean isJson() {
        return raw.metadata().isJson();
    }

    @Override
    public boolean hasDefaultValue() {
        return raw.metadata().hasDefaultValue();
    }

    @Override
    public Object getDefaultValue() {
        return raw.metadata().getDefaultValue();
    }

    @Override
    public Class<?> getSqlType() {
        return raw.metadata().getSqlType();
    }

    @Override
    public String getSqlTypeName() {
        return raw.metadata().getSqlTypeName();
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        builder.sql(alias).sql(".").sql(metadata().getColumnName());
    }
}
