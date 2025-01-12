package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

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

    @Override
    public boolean isInsertable(Collection<ImmutableProp> conflictProps, @Nullable UpsertMask<?> mask) {
        if (conflictProps.contains(prop)) {
            return true;
        }
        if (mask == null) {
            return true;
        }
        return isMutableByPaths(mask.getInsertablePaths());
    }

    @Override
    public boolean isUpdatable(Collection<ImmutableProp> conflictProps, @Nullable UpsertMask<?> mask) {
        if (conflictProps.contains(prop)) {
            return false;
        }
        if (mask == null) {
            return true;
        }
        return isMutableByPaths(mask.getUpdatablePaths());
    }

    private boolean isMutableByPaths(@Nullable List<List<ImmutableProp>> paths) {
        if (paths == null) {
            return true;
        }
        for (List<ImmutableProp> path : paths) {
            if (isMutableByPath(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMutableByPath(@Nullable List<ImmutableProp> path) {
        if (!prop.equals(path.get(0))) {
            return false;
        }
        if (path.size() > 1) {
            if (!(valueGetter instanceof EmbeddedValueGetter)) {
                return false;
            }
            List<ImmutableProp> deeperProps = path.subList(1, path.size());
            List<ImmutableProp> currentDeeperProps = ((EmbeddedValueGetter)valueGetter).props();
            int size = deeperProps.size();
            if (size > currentDeeperProps.size()) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (!deeperProps.get(i).equals(currentDeeperProps.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    abstract String toStringPrefix();
}
