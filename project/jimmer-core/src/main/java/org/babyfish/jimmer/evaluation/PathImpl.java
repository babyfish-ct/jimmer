package org.babyfish.jimmer.evaluation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class PathImpl implements Path {

    private final Root<?> root;

    private final PathImpl parent;

    private final ImmutableSpi source;

    private final ImmutableProp prop;

    private final int index;

    private final boolean loaded;

    private final Object value;

    private String str;

    PathImpl(
            Object parent,
            ImmutableSpi source,
            ImmutableProp prop,
            int index,
            boolean loaded,
            Object value
    ) {
        if (parent instanceof Root) {
            this.root = (Root<?>) parent;
            this.parent = null;
        } else {
            PathImpl parentCtx = (PathImpl) parent;
            this.root = parentCtx.root;
            this.parent = parentCtx;
        }
        this.source = source;
        this.prop = prop;
        this.index = index;
        this.loaded = loaded;
        this.value = value;
    }

    @Nullable
    @Override
    public Path getParent() {
        return parent;
    }

    @NotNull
    @Override
    public ImmutableSpi getSource() {
        return source;
    }

    @NotNull
    @Override
    public ImmutableProp getProp() {
        return prop;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        String s = this.str;
        if (s == null) {
            StringBuilder builder = new StringBuilder();
            toString("<root>", builder);
            this.str = s = builder.toString();
        }
        return s;
    }

    public String toString(String rootName) {
        if (rootName == null || rootName.isEmpty()) {
            return toString();
        }
        StringBuilder builder = new StringBuilder();
        toString(rootName, builder);
        return builder.toString();
    }

    private void toString(String rootName, StringBuilder builder) {
        if (parent != null) {
            parent.toString(rootName, builder);
        } else {
            builder.append(rootName);
        }
        if (index != -1) {
            if (prop.getCategory() == ImmutablePropCategory.REFERENCE_LIST) {
                builder.append('[').append(index).append(']');
            }
        } else {
            builder.append('.').append(prop.getName());
        }
    }
}
