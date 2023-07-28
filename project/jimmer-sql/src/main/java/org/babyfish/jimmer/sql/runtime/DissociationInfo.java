package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public final class DissociationInfo {

    @NotNull
    private final List<ImmutableProp> props;

    @NotNull
    private final List<ImmutableProp> backProps;

    public DissociationInfo(List<ImmutableProp> props, List<ImmutableProp> backProps) {
        this.props = Objects.requireNonNull(props, "`props` cannot be null");
        this.backProps = Objects.requireNonNull(backProps, "`backProps` cannot be null");
    }

    public List<ImmutableProp> getProps() {
        return props;
    }

    public List<ImmutableProp> getBackProps() {
        return backProps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(props, backProps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DissociationInfo that = (DissociationInfo) o;
        return Objects.equals(props, that.props) && Objects.equals(backProps, that.backProps);
    }

    @Override
    public String toString() {
        return "DissociationInfo{" +
                "props=" + props +
                ", backProps=" + backProps +
                '}';
    }
}
