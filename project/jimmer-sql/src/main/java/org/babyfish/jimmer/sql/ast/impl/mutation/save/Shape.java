package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MultipleColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.*;

class Shape {

    private final ImmutableType type;

    private final List<PropertyGetter> getters;

    private final int hash;
    
    private Map<ImmutableProp, List<PropertyGetter>> getterMap;

    private Set<PropertyGetter> getterSet;

    private Shape(ImmutableType type, List<PropertyGetter> getters) {
        this.type = type;
        this.getters = getters;
        this.hash = getters.hashCode();
    }

    public static Shape of(JSqlClientImplementor sqlClient, ImmutableSpi spi) {
        return new Shape(
                spi.__type(), 
                PropertyGetter.entityGetters(sqlClient, spi.__type(), spi)
        );
    }

    public static Shape fullOf(JSqlClientImplementor sqlClient, Class<?> type) {
        ImmutableType immutableType = ImmutableType.get(type);
        return new Shape(immutableType, PropertyGetter.entityGetters(sqlClient, immutableType, null));
    }

    public ImmutableType getType() {
        return type;
    }

    public List<PropertyGetter> getGetters() {
        return getters;
    }

    public Map<ImmutableProp, List<PropertyGetter>> getGetterMap() {
        Map<ImmutableProp, List<PropertyGetter>> getterMap = this.getterMap;
        if (getterMap == null) {
            getterMap = new TreeMap<>(Comparator.comparing(ImmutableProp::getName));
            for (PropertyGetter getter : getters) {
                getterMap.computeIfAbsent(getter.prop(), it -> new ArrayList<>()).add(getter);
            }
            for (Map.Entry<ImmutableProp, List<PropertyGetter>> e : getterMap.entrySet()) {
                e.setValue(Collections.unmodifiableList(e.getValue()));
            }
            this.getterMap = getterMap = Collections.unmodifiableMap(getterMap);
        }
        return getterMap;
    }

    public List<PropertyGetter> getIdGetters() {
        return propertyGetters(type.getIdProp());
    }

    public PropertyGetter getVersionGetter() {
        List<PropertyGetter> items = propertyGetters(type.getVersionProp());
        return items.isEmpty() ? null : items.get(0);
    }

    public List<PropertyGetter> propertyGetters(ImmutableProp prop) {
        List<PropertyGetter> items;
        if (prop == null) {
            items = null;
        } else {
            items = getGetterMap().get(prop);
        }
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    public boolean contains(PropertyGetter getter) {
        Set<PropertyGetter> set = this.getterSet;
        if (set == null) {
            this.getterSet = set = new HashSet<>(getters);
        }
        return set.contains(getter);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Shape)) {
            return false;
        }
        Shape other = (Shape) obj;
        return hash == other.hash && getters.equals(other.getters);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean addComma = false;
        for (PropertyGetter getter : getters) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(getter);
        }
        builder.append(']');
        return builder.toString();
    }
}
