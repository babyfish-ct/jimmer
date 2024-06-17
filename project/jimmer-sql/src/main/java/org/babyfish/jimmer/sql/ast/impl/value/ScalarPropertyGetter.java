package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class ScalarPropertyGetter extends AbstractPropertyGetter {

    private ScalarPropertyGetter(@Nullable String alias, ImmutableProp prop, ValueGetter valueGetter) {
        super(alias, prop, valueGetter);
    }

    @Override
    public Object get(Object value) {
        ImmutableSpi spi = (ImmutableSpi) value;
        return valueGetter.get(spi.__get(prop.getId()));
    }

    @Override
    String toStringPrefix() {
        return prop.getName();
    }

    static List<PropertyGetter> getters(@Nullable String alias, ImmutableProp prop, List<ValueGetter> valueGetters) {
        List<PropertyGetter> propertyGetters = new ArrayList<>(valueGetters.size());
        for (ValueGetter valueGetter : valueGetters) {
            propertyGetters.add(new ScalarPropertyGetter(alias, prop, valueGetter));
        }
        return propertyGetters;
    }
}
