package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class CacheDisableConfig {

    private boolean disableAll;

    private final Set<ImmutableType> disabledTypes = new HashSet<>();

    private final Set<ImmutableProp> disabledProps = new HashSet<>();

    public void disableAll() {
        disableAll = true;
    }

    public void disable(Class<?> entityType) {
        disabledTypes.add(ImmutableType.get(entityType));
    }

    public void disable(ImmutableType type) {
        disabledTypes.add(type);
    }

    public <ST extends Table<?>> void disable(TypedProp<?, ?> prop) {
        disabledProps.add(prop.unwrap());
    }

    public void disable(ImmutableProp prop) {
        disabledProps.add(prop);
    }

    boolean isDisableAll() {
        return disableAll;
    }

    Set<ImmutableType> getDisabledTypes() {
        return disabledTypes;
    }

    Set<ImmutableProp> getDisabledProps() {
        return disabledProps;
    }
}
