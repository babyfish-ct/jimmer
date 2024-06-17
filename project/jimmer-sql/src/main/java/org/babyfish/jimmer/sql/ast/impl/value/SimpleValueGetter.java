package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

public class SimpleValueGetter extends AbstractValueGetter {

    private final String columnName;

    private final ImmutableProp valueProp;

    SimpleValueGetter(String columnName, ImmutableProp valueProp, ScalarProvider<Object, Object> scalarProvider) {
        super(scalarProvider);
        this.columnName = columnName;
        this.valueProp = valueProp;
    }

    @Override
    public String columnName() {
        return columnName;
    }

    @Override
    protected Object scalar(Object value) {
        return value;
    }

    @Override
    public int hashCode() {
        return columnName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleValueGetter)) {
            return false;
        }
        SimpleValueGetter other = (SimpleValueGetter) obj;
        return columnName.equals(other.columnName);
    }

    @Override
    public String toString() {
        return "SimpleScalarGetter{" +
                "columnName='" + columnName + '\'' +
                '}';
    }

    @Override
    protected final ImmutableProp valueProp() {
        return valueProp;
    }
}
