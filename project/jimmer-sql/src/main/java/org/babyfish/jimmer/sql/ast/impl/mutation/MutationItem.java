package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface MutationItem {

    ImmutableProp getProp();

    List<ImmutableProp> getProps();

    Object getValue();

    String columnName(MetadataStrategy strategy);

    PropExpression<Object> expression(Table<?> table);

    /**
     * When property is an entity reference property, `value` can be
     * either associated object or associated id
     */
    static List<MutationItem> create(ImmutableProp prop, Object value) {
        if (prop.isReference(TargetLevel.ENTITY)) {
            ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
            // Important value can be either associated object or associated id
            if (value instanceof ImmutableSpi && ((ImmutableSpi)value).__type().isEntity()) {
                ImmutableSpi spi = (ImmutableSpi) value;
                PropId idPropId = targetIdProp.getId();
                if (spi.__isLoaded(idPropId)) {
                    value = spi.__get(idPropId);
                }
            }
            if (targetIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                return ChainedMutationItemImpl.expand(prop, targetIdProp.getTargetType(), value);
            }
            return Collections.singletonList(
                    new ChainedMutationItemImpl(
                            Arrays.asList(prop, targetIdProp),
                            value,
                            0
                    ) {
                        @Override
                        public PropExpression<Object> expression(Table<?> table) {
                            return table.getAssociatedId(prop);
                        }
                    }
            );
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return ChainedMutationItemImpl.expand(prop, prop.getTargetType(), value);
        }
        return Collections.singletonList(new SingleMutationImpl(prop, value));
    }
}

class SingleMutationImpl implements MutationItem {

    private final ImmutableProp prop;

    private final Object value;

    SingleMutationImpl(ImmutableProp prop, Object value) {
        this.prop = prop;
        this.value = value;
    }

    @Override
    public ImmutableProp getProp() {
        return prop;
    }

    @Override
    public List<ImmutableProp> getProps() {
        return Collections.singletonList(prop);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String columnName(MetadataStrategy strategy) {
        return prop.<SingleColumn>getStorage(strategy).getName();
    }

    @Override
    public PropExpression<Object> expression(Table<?> table) {
        return table.get(prop);
    }

    @Override
    public String toString() {
        return "SingleMutationImpl{" +
                "prop=" + prop +
                ", value=" + value +
                '}';
    }
}

class ChainedMutationItemImpl implements MutationItem {

    private final List<ImmutableProp> props;

    private final Object value;

    private final int columnIndex;

    ChainedMutationItemImpl(List<ImmutableProp> props, Object value, int columnIndex) {
        if (value == Context.UNLOADED) {
            throw new AssertionError("The value should not be UNLOADED flag");
        }
        this.props = props;
        this.value = value;
        this.columnIndex = columnIndex;
    }

    @Override
    public ImmutableProp getProp() {
        return props.get(props.size() - 1);
    }

    @Override
    public List<ImmutableProp> getProps() {
        return props;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String columnName(MetadataStrategy strategy) {
        return props.get(0).<ColumnDefinition>getStorage(strategy).name(columnIndex);
    }

    @Override
    public PropExpression<Object> expression(Table<?> table) {
        PropExpression<Object> expr = null;
        for (ImmutableProp prop : props) {
            if (expr != null) {
                expr = ((PropExpression.Embedded<Object>) expr).get(prop);
            } else {
                expr = table.get(prop);
            }
        }
        return expr;
    }

    static List<MutationItem> expand(ImmutableProp parentProp, ImmutableType type, Object obj) {
        Context ctx = new Context();
        if (parentProp != null) {
            ctx.push(parentProp);
        }
        collectItems(type, (ImmutableSpi) obj, ctx);
        return ctx.toMutationItems();
    }

    private static void collectItems(ImmutableType type, Object obj, Context ctx) {
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isFormula()) {
                continue;
            }
            Object value;
            if (obj != null && obj != Context.UNLOADED) {
                ImmutableSpi spi = (ImmutableSpi) obj;
                PropId propId = prop.getId();
                if (spi.__isLoaded(propId)) {
                    value = spi.__get(propId);
                } else {
                    value = Context.UNLOADED;
                }
            } else {
                value = obj;
            }
            if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
                ctx.push(prop);
                try {
                    collectItems(prop.getTargetType(), (ImmutableSpi) value, ctx);
                } finally {
                    ctx.pop();
                }
            } else {
                ctx.addResult(prop, value);
            }
        }
    }

    @Override
    public String toString() {
        return "ChainedMutationItemImpl{" +
                "props=" + props +
                ", value=" + value +
                ", columnIndex=" + columnIndex +
                '}';
    }

    private static class Context {

        private static final Object UNLOADED = new Object();

        private List<ImmutableProp> stack = new ArrayList<>();

        private int columnIndex;

        private List<MutationItem> results = new ArrayList<>();

        public void push(ImmutableProp prop) {
            stack.add(prop);
        }

        public void pop() {
            stack.remove(stack.size() - 1);
        }

        public void addResult(ImmutableProp prop, Object value) {
            if (value == UNLOADED) {
                columnIndex++;
                return;
            }
            List<ImmutableProp> props = new ArrayList<>(stack.size() + 1);
            props.addAll(stack);
            props.add(prop);
            MutationItem item = new ChainedMutationItemImpl(
                    Collections.unmodifiableList(props),
                    value,
                    columnIndex++
            );
            results.add(item);
        }

        public List<MutationItem> toMutationItems() {
            return Collections.unmodifiableList(results);
        }
    }
}

