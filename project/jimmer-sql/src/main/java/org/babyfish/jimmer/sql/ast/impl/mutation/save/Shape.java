package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MultipleColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;

import java.util.*;

class Shape {

    private static final ClassCache<Shape> FULL_SHAPE_CACHE =
            new ClassCache<>(Shape::createFullShape);

    private static final Item NIL_ITEM = new NilItem();

    private final ImmutableType type;

    private final List<Item> items;

    private Map<ImmutableProp, List<Item>> itemMap;

    private final int hash;

    private Set<Item> itemSet;

    private List<Item> idItems;

    private Item versionItem;

    private Shape(ImmutableType type, List<Item> items) {
        this.type = type;
        this.items = items;
        this.hash = items.hashCode();
    }

    public static Shape of(ImmutableSpi spi) {
        Scope scope = new Scope();
        scope.collect(spi.__type(), spi);
        return new Shape(spi.__type(), Collections.unmodifiableList(scope.toItems()));
    }

    public static Shape fullOf(Class<?> type) {
        return FULL_SHAPE_CACHE.get(type);
    }

    public ImmutableType getType() {
        return type;
    }

    public List<Item> getItems() {
        return items;
    }

    public Map<ImmutableProp, List<Item>> getItemMap() {
        Map<ImmutableProp, List<Item>> itemMap = this.itemMap;
        if (itemMap == null) {
            itemMap = new TreeMap<>(Comparator.comparing(ImmutableProp::getName));
            for (Item item : items) {
                itemMap.computeIfAbsent(item.prop(), it -> new ArrayList<>()).add(item);
            }
            for (Map.Entry<ImmutableProp, List<Item>> e : itemMap.entrySet()) {
                e.setValue(Collections.unmodifiableList(e.getValue()));
            }
            this.itemMap = itemMap = Collections.unmodifiableMap(itemMap);
        }
        return itemMap;
    }

    public List<Item> getIdItems() {
        return propItems(type.getIdProp());
    }

    public Item getVersionItem() {
        List<Item> items = propItems(type.getVersionProp());
        return items.isEmpty() ? null : items.get(0);
    }

    public List<Item> propItems(ImmutableProp prop) {
        List<Item> items;
        if (prop == null) {
            items = null;
        } else {
            items = getItemMap().get(prop);
        }
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    public boolean contains(Item item) {
        Set<Item> set = this.itemSet;
        if (set == null) {
            this.itemSet = set = new HashSet<>(items);
        }
        return set.contains(item);
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
        if (!(obj instanceof Shape)) {
            return false;
        }
        Shape other = (Shape) obj;
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

    private static Shape createFullShape(Class<?> type) {
        ImmutableType immutableType = ImmutableType.get(type);
        Scope scope = new Scope();
        scope.collect(immutableType, null);
        return new Shape(immutableType, Collections.unmodifiableList(scope.toItems()));
    }

    static Item item(ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            throw new IllegalArgumentException("\"prop\" cannot be embedded");
        }
        if (prop.isReference(TargetLevel.ENTITY)) {
            return new SimpleReferenceItem(prop);
        }
        return new SimpleScalarItem(prop);
    }

    interface Item {
        Object get(ImmutableSpi spi);
        String columnName(MetadataStrategy strategy);
        List<ImmutableProp> props();
        ImmutableProp prop();
        ImmutableProp deepestProp();
        boolean isNullable();
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
        public List<ImmutableProp> props() {
            return Collections.singletonList(prop);
        }

        @Override
        public ImmutableProp prop() {
            return prop;
        }

        @Override
        public ImmutableProp deepestProp() {
            return prop;
        }

        @Override
        public boolean isNullable() {
            return prop.isNullable();
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

        private final ImmutableProp targetIdProp;

        SimpleReferenceItem(ImmutableProp prop) {
            this.prop = prop;
            this.targetIdProp = prop.getTargetType().getIdProp();
        }

        @Override
        public Object get(ImmutableSpi spi) {
            PropId propId = prop.getId();
            Object target = spi.__get(propId);
            if (target == null) {
                return null;
            }
            return ((ImmutableSpi)target).__get(targetIdProp.getId());
        }

        @Override
        public String columnName(MetadataStrategy strategy) {
            return prop.<SingleColumn>getStorage(strategy).getName();
        }

        @Override
        public List<ImmutableProp> props() {
            return Arrays.asList(prop, targetIdProp);
        }

        @Override
        public ImmutableProp prop() {
            return prop;
        }

        @Override
        public ImmutableProp deepestProp() {
            return targetIdProp;
        }

        @Override
        public boolean isNullable() {
            return prop.isNullable();
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
            return prop.getName() + '.' + targetIdProp.getName();
        }
    }

    private static abstract class AbstractCompositeItem implements Item {

        final ImmutableProp[] props;

        private final int index;

        private Boolean isNullable;

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
        public List<ImmutableProp> props() {
            return Collections.unmodifiableList(Arrays.asList(props));
        }

        @Override
        public ImmutableProp prop() {
            return props[0];
        }

        @Override
        public ImmutableProp deepestProp() {
            return props[props.length - 1];
        }

        @Override
        public boolean isNullable() {
            Boolean isNullable = this.isNullable;
            if (isNullable == null) {
                boolean result = false;
                for (ImmutableProp prop : props) {
                    if (prop.isNullable()) {
                        result = true;
                        break;
                    }
                }
                this.isNullable = isNullable = result;
            }
            return isNullable;
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
    }

    private static class NilItem implements Item {

        @Override
        public Object get(ImmutableSpi spi) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String columnName(MetadataStrategy strategy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ImmutableProp> props() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ImmutableProp prop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ImmutableProp deepestProp() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNullable() {
            throw new UnsupportedOperationException();
        }
    }

    private static class Scope {

        private static final ImmutableProp[] EMPTY_PROPS = new ImmutableProp[0];

        private final Scope parent;

        private final ImmutableProp prop;

        private final boolean loaded;

        private final List<Item> items;

        private final EmbeddedIndex index;

        Scope() {
            this.parent = null;
            this.prop = null;
            this.loaded = true;
            this.items = new ArrayList<>();
            this.index = new EmbeddedIndex();
        }

        private Scope(Scope parent, ImmutableProp prop, boolean loaded) {
            this.parent = parent;
            this.prop = prop;
            this.loaded = parent.loaded && loaded;
            this.items = parent.items;
            this.index = parent.prop != null ? parent.index : new EmbeddedIndex();
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
                    new Scope(
                            new Scope(
                                    this,
                                    prop,
                                    isLoaded
                            ),
                            targetIdProp,
                            target == null || target.__isLoaded(targetIdProp.getId())
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
                        items.add(new Scope(this, prop, true).collectTerminal(prop));
                    }
                    index.increase();
                }
            }
        }

        private Item collectTerminal(ImmutableProp prop) {
            if (parent == null || parent.prop == null) {
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
                return new MultipleColumnsJoinItem(arr, index.get());
            }
            return new EmbeddableScalarItem(arr, index.get());
        }

        private boolean isLoaded(ImmutableProp prop, ImmutableSpi owner) {
            if (!loaded) {
                return false;
            }
            return owner == null || owner.__isLoaded(prop.getId());
        }
    }

    private static class EmbeddedIndex {

        private int value;

        int get() {
            return value;
        }

        void increase() {
            value++;
        }
    }
}
