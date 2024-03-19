package org.babyfish.jimmer.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Dependency {

    @NotNull
    private final List<ImmutableProp> props;

    public Dependency(List<ImmutableProp> props) {
        this.props = props;
    }

    public Dependency(ImmutableProp ... props) {
        List<ImmutableProp> list = new ArrayList<>(props.length);
        for (ImmutableProp prop : props) {
            list.add(prop);
        }
        this.props = Collections.unmodifiableList(list);
    }

    @NotNull
    public List<ImmutableProp> getProps() {
        return props;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency)) return false;
        Dependency that = (Dependency) o;
        return props.equals(that.props);
    }

    @Override
    public int hashCode() {
        return props.hashCode();
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "props=" + props +
                '}';
    }
}
