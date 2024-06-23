package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

class TupleValueGetter implements ValueGetter {

    private final int index;

    private final ValueGetter next;

    TupleValueGetter(int index, ValueGetter next) {
        this.index = index;
        this.next = next;
    }

    @Override
    public Object get(Object value) {
        return next.get(((TupleImplementor) value).get(index));
    }

    @Override
    public GetterMetadata metadata() {
        return next.metadata();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(index) * 31 + next.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TupleValueGetter)) {
            return false;
        }
        TupleValueGetter other = (TupleValueGetter) obj;
        return index == other.index && next.equals(other.next);
    }

    @Override
    public String toString() {
        return "Tuple[" + index + "]." + next;
    }
}
