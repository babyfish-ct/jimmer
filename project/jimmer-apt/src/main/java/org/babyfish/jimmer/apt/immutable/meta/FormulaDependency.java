package org.babyfish.jimmer.apt.immutable.meta;

import java.util.List;

public class FormulaDependency {

    private final List<ImmutableProp> props;

    public FormulaDependency(List<ImmutableProp> props) {
        this.props = props;
    }

    public List<ImmutableProp> getProps() {
        return props;
    }

    @Override
    public int hashCode() {
        return props.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaDependency that = (FormulaDependency) o;
        return props.equals(that.props);
    }

    @Override
    public String toString() {
        return "FormulaDependency{" +
                "props=" + props +
                '}';
    }
}
