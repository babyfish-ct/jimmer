package org.babyfish.jimmer.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Dependency {

    @NotNull
    private final ImmutableProp prop;

    @Nullable
    private final ImmutableProp deeperProp;

    public Dependency(@NotNull ImmutableProp prop) {
        this.prop = prop;
        this.deeperProp = null;
    }

    public Dependency(@NotNull ImmutableProp prop, @Nullable ImmutableProp deeperProp) {
        this.prop = prop;
        this.deeperProp = deeperProp;
    }

    @NotNull
    public ImmutableProp getProp() {
        return prop;
    }

    @Nullable
    public ImmutableProp getDeeperProp() {
        return deeperProp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency)) return false;
        Dependency that = (Dependency) o;
        return prop.equals(that.prop) && Objects.equals(deeperProp, that.deeperProp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop, deeperProp);
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "prop=" + prop +
                ", deeperProp=" + deeperProp +
                '}';
    }
}
