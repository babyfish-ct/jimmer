package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class ScalarPropertyGetter extends AbstractPropertyGetter {

    private ScalarPropertyGetter(@Nullable String alias, ImmutableProp prop, ValueGetter valueGetter) {
        super(alias, prop, valueGetter);
    }

    @Override
    public Object get(Object value) {
        ImmutableSpi spi = (ImmutableSpi) value;
        PropId propId = prop.getId();
        if (spi.__isLoaded(propId) && !prop.isLogicalDeleted()) {
            return valueGetter.get(spi.__get(propId));
        }
        Ref<Object> valueRef = prop.getDefaultValueRef();
        if (valueRef != null) {
            Object v = valueRef.getValue();
            Object evaluatedValue = v instanceof Supplier<?> ? ((Supplier<?>) v).get() : v;
            return valueGetter.get(evaluatedValue);
        }
        return spi.__get(propId);
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
