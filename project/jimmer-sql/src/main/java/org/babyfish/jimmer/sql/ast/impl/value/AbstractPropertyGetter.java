package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.Nullable;

abstract class AbstractPropertyGetter implements PropertyGetter {

    @Nullable
    final String alias;

    final ImmutableProp prop;

    final ValueGetter valueGetter;

    AbstractPropertyGetter(@Nullable String alias, ImmutableProp prop, ValueGetter valueGetter) {
        this.alias = alias;
        this.prop = prop;
        this.valueGetter = valueGetter;
    }

    @Override
    public @Nullable String alias() {
        return alias;
    }

    @Override
    public ImmutableProp prop() {
        return prop;
    }

    @Override
    public String columnName() {
        return valueGetter.columnName();
    }

    @Override
    public GetterMetadata metadata() {
        return valueGetter.metadata();
    }

    @Override
    public int hashCode() {
        return prop.hashCode() * 31 + valueGetter.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractPropertyGetter)) {
            return false;
        }
        AbstractPropertyGetter other = (AbstractPropertyGetter) obj;
        return prop.equals(other.prop) && valueGetter.equals(other.valueGetter);
    }

    @Override
    public final String toString() {
        if (valueGetter instanceof EmbeddedValueGetter) {
            return toStringPrefix() + '.' + valueGetter;
        }
        return toStringPrefix();
    }

    abstract String toStringPrefix();
}
