package org.babyfish.jimmer.meta;


import java.util.Objects;

public class OrderedItem {

    private final ImmutableProp prop;

    private final boolean desc;
    private final NullOrderMode nullsOrder;

    public OrderedItem(ImmutableProp prop, boolean desc, NullOrderMode nullsOrder) {
        this.prop = prop;
        this.desc = desc;
        this.nullsOrder = nullsOrder;
    }

    public ImmutableProp getProp() {
        return prop;
    }

    public NullOrderMode getNullsOrder() {
        return nullsOrder;
    }

    public boolean isDesc() {
        return desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderedItem that = (OrderedItem) o;
        return desc == that.desc && prop.equals(that.prop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop, desc);
    }

    @Override
    public String toString() {
        return "OrderedItem{" +
                "prop=" + prop +
                ", desc=" + desc +
                '}';
    }
}
