package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MultipleColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;

import java.util.*;

class SaveShape {

    private final ImmutableType type;

    private final List<Item> items;

    private final int hash;

    private SaveShape(ImmutableType type, List<Item> items) {
        this.type = type;
        this.items = items;
        this.hash = items.hashCode();
    }

    public static SaveShape of(ImmutableSpi spi) {
        Scope scope = new Scope();
        scope.collect(spi.__type(), spi);
        return new SaveShape(spi.__type(), Collections.unmodifiableList(scope.toItems()));
    }

    public ImmutableType getType() {
        return type;
    }

    public List<Item> getProps() {
        return items;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SaveShape)) {
            return false;
        }
        SaveShape other = (SaveShape) obj;
        return hash == other.hash && items.equals(other.items);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean addComma = false;
        for (Item item : items) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(item);
        }
        builder.append(']');
        return builder.toString();
    }

    interface Item {
        Object get(ImmutableSpi spi);
        String columnName(MetadataStrategy strategy);
    }

    private static class SimpleScalarItem implements Item {

        private final ImmutableProp prop;

        SimpleScalarItem(ImmutableProp prop) {
            this.prop = prop;
        }

        @Override
        public Object get(ImmutableSpi spi) {
            return spi.__get(prop.getId());
        }

        @Override
        public String columnName(MetadataStrategy strategy) {
            return prop.<SingleColumn>getStorage(strategy).getName();
        }

        @Override
        public int hashCode() {
            return prop.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleScalarItem that = (SimpleScalarItem) o;
            return prop.equals(that.prop);
        }

        @Override
        public String toString() {
            return prop.getName();
        }
    }

    private static class SimpleReferenceItem implements Item {

        private final ImmutableProp prop;

        private final PropId targetIdPropId;

        SimpleReferenceItem(ImmutableProp prop) {
            this.prop = prop;
            this.targetIdPropId = prop.getTargetType().getIdProp().getId();
        }

        @Override
        public Object get(ImmutableSpi spi) {
            PropId propId = prop.getId();
            Object target = spi.__get(propId);
            if (target == null) {
                return null;
            }
            return ((ImmutableSpi)target).__get(targetIdPropId);
        }

        @Override
        public String columnName(MetadataStrategy strategy) {
            return prop.<SingleColumn>getStorage(strategy).getName();
        }

        @Override
        public int hashCode() {
            return prop.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleReferenceItem that = (SimpleReferenceItem) o;
            return prop.equals(that.prop);
        }

        @Override
        public String toString() {
            return prop.getName();
        }
    }

    private static abstract class AbstractCompositeItem implements Item {

        final ImmutableProp[] props;

        private final int index;

        AbstractCompositeItem(ImmutableProp[] props, int index) {
            this.props = props;
            this.index = index;
        }

        @Override
        public Object get(ImmutableSpi spi) {
            Object value = spi;
            for (ImmutableProp prop : props) {
                value = ((ImmutableSpi) value).__get(prop.getId());
                if (value == null) {
                    return null;
                }
            }
            return value;
        }

        @Override
        public String columnName(MetadataStrategy strategy) {
            return props[0].<MultipleColumns>getStorage(strategy).name(index);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(props);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AbstractCompositeItem that = (AbstractCompositeItem) o;
            return Arrays.equals(props, that.props);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            int len = props.length;
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    builder.append('.');
                }
                builder.append(props[i].getName());
            }
            return builder.toString();
        }
    }

    private static class EmbeddableScalarItem extends AbstractCompositeItem {

        EmbeddableScalarItem(ImmutableProp[] props, int index) {
            super(props, index);
        }
    }

    private static class MultipleColumnsJoinItem extends AbstractCompositeItem {

        MultipleColumnsJoinItem(ImmutableProp[] props, int index) {
            super(props, index);
        }

        @Override
        public Object get(ImmutableSpi spi) {
            Object value = spi.__get(props[0].getId());
            if (value == null) {
                return null;
            }
            return super.get((ImmutableSpi) value);
        }
    }

    private static class Scope {

        private static final ImmutableProp[] EMPTY_PROPS = new ImmutableProp[0];

        private final Scope parent;

        private final ImmutableProp prop;

        private final boolean loaded;

        private final List<Item> items;

        private int index;

        Scope() {
            this.parent = null;
            this.prop = null;
            this.loaded = true;
            this.items = new ArrayList<>();
        }

        private Scope(Scope parent, ImmutableProp prop, boolean loaded) {
            this.parent = parent;
            this.prop = prop;
            this.loaded = parent.loaded && loaded;
            this.items = parent.items;
        }

        public List<Item> toItems() {
            return items;
        }

        public void collect(ImmutableType type, ImmutableSpi spi) {
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isFormula()) {
                    continue;
                }
                if (prop.isView()) {
                    continue;
                }
                PropId propId = prop.getId();
                boolean isLoaded = isLoaded(prop, spi);
                if (prop.isEmbedded(EmbeddedLevel.REFERENCE)) {
                    ImmutableSpi target = isLoaded && spi != null ?
                            (ImmutableSpi) spi.__get(propId) :
                            null;
                    ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
                    isLoaded &= target == null || target.__isLoaded(targetIdProp.getId());
                    new Scope(
                            this,
                            prop,
                            isLoaded
                    ).collect(
                            targetIdProp.getTargetType(),
                            target == null ? null : (ImmutableSpi) target.__get(targetIdProp.getId())
                    );
                } else if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    ImmutableSpi value = isLoaded && spi != null ?
                            (ImmutableSpi) spi.__get(propId) :
                            null;
                    new Scope(
                            this,
                            prop,
                            isLoaded
                    ).collect(
                            prop.getTargetType(),
                            value
                    );
                } else {
                    if (isLoaded) {
                        items.add(new Scope(this, prop, true).collectTerminal(prop, spi));
                    }
                    index++;
                }
            }
        }

        private Item collectTerminal(ImmutableProp prop, ImmutableSpi owner) {
            if (parent.prop == null) {
                if (prop.isReference(TargetLevel.ENTITY)) {
                    return new SimpleReferenceItem(prop);
                }
                return new SimpleScalarItem(prop);
            }
            List<ImmutableProp> props = new LinkedList<>();
            for (Scope scope = this; scope != null && scope.prop != null; scope = scope.parent) {
                props.add(0, scope.prop);
            }
            ImmutableProp[] arr = props.toArray(EMPTY_PROPS);
            if (arr[0].isReference(TargetLevel.ENTITY)) {
                return new MultipleColumnsJoinItem(arr, index);
            }
            return new EmbeddableScalarItem(arr, index);
        }

        private boolean isLoaded(ImmutableProp prop, ImmutableSpi owner) {
            if (!loaded) {
                return false;
            }
            return owner == null || owner.__isLoaded(prop.getId());
        }
    }
}
