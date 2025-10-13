package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.meta.impl.Utils;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.PropOverride;
import org.babyfish.jimmer.sql.PropOverrides;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;

import java.util.*;

class EmbeddedTree {

    private final EmbeddedTree parent;

    private final ImmutableProp prop;

    private final String path;

    private final int depth;

    private final Map<String, EmbeddedTree> childMap;

    private OverrideContext usedCtx;

    public EmbeddedTree(ImmutableProp prop) {
        this(null, prop);
        applyOverride();
    }

    private EmbeddedTree(EmbeddedTree parent, ImmutableProp prop) {
        for (EmbeddedTree p = parent; p != null; p = p.parent) {
            if (p.prop.getDeclaringType() == prop.getTargetType()) {
                Deque<String> names = new ArrayDeque<>();
                for (EmbeddedTree p2 = parent; p2 != null; p2 = p2.parent) {
                    names.addFirst(p2.prop.getName());
                    if (p2 == p) {
                        break;
                    }
                }
                throw new ModelException(
                        "Reference cycle is found in \"" +
                                p.prop.getDeclaringType() +
                                '.' +
                                String.join(".", names) +
                                '.' +
                                prop.getName() +
                                "\""
                );
            }
        }
        this.parent = parent;
        this.prop = prop;
        if (parent == null) {
            this.path = "";
            depth = 0;
        } else {
            String parentPath = parent.path;
            if (parentPath.isEmpty()) {
                this.path = prop.getName();
            } else {
                this.path = parentPath + '.' + prop.getName();
            }
            depth = parent.depth + 1;
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            Map<String, EmbeddedTree> map = new LinkedHashMap<>();
            for (ImmutableProp childProp : prop.getTargetType().getProps().values()) {
                map.put(childProp.getName(), new EmbeddedTree(this, childProp));
            }
            this.childMap = map;
        } else {
            this.childMap = Collections.emptyMap();
        }
    }

    private void applyOverride() {
        PropOverrides propOverrides = prop.getAnnotation(PropOverrides.class);
        if (propOverrides != null) {
            for (PropOverride propOverride : propOverrides.value()) {
                applyOverride(propOverride.prop(), new OverrideContext(prop, depth, propOverride));
            }
        }
        PropOverride propOverride = prop.getAnnotation(PropOverride.class);
        if (propOverride != null) {
            applyOverride(propOverride.prop(), new OverrideContext(prop, depth, propOverride));
        }
        for (EmbeddedTree childTree : childMap.values()) {
            childTree.applyOverride();
        }
    }

    private void applyOverride(String path, OverrideContext ctx) {
        String propName;
        String rest;
        int index = path.indexOf('.');
        if (index == -1) {
            propName = path;
            rest = null;
        } else {
            propName = path.substring(0, index);
            rest = path.substring(index + 1);
        }
        EmbeddedTree childTree = childMap.get(propName);
        if (childTree == null) {
            throw new ModelException(
                    "Illegal property \"" +
                            ctx.prop +
                            "\", the path \"" +
                            ctx.annotation.prop() +
                            "\" of `@PropOverride` is illegal, there is no property \"" +
                            propName +
                            "\" declared in \"" +
                            prop.getDeclaringType() +
                            "\""
            );
        }
        boolean tooShort = rest == null && childTree.prop.isEmbedded(EmbeddedLevel.SCALAR);
        boolean tooLong = rest != null && !childTree.prop.isEmbedded(EmbeddedLevel.SCALAR);
        if (tooLong || tooShort) {
            throw new ModelException(
                    "Illegal property \"" +
                            ctx.prop +
                            "\", the property path \"" +
                            ctx.annotation.prop() +
                            "\" of `@PropOverride` is too " +
                            (tooLong ? "long" : "short")
            );
        }
        if (rest == null) {
            childTree.useOverride(ctx);
        } else {
            childTree.applyOverride(rest, ctx);
        }
    }

    private void useOverride(OverrideContext ctx) {
        if (usedCtx == null || ctx.depth < usedCtx.depth) {
            usedCtx = ctx;
        } else if (this.usedCtx.depth == ctx.depth) {
            throw new ModelException(
                    "Illegal property \"" +
                            ctx.prop +
                            "\", the property path \"" +
                            ctx.annotation.prop() +
                            "\" and \"" +
                            usedCtx.annotation.prop() +
                            "\" of `@PropOverride`s are conflict"
            );
        }
    }

    public EmbeddedColumns toEmbeddedColumns(MetadataStrategy strategy) {
        CollectContext ctx = new CollectContext(prop, strategy);
        collect(ctx);
        return ctx.toEmbeddedColumns(prop.getTargetType());
    }

    private void collect(CollectContext ctx) {
        ctx.accept(this);
        for (EmbeddedTree childTree : childMap.values()) {
            childTree.collect(ctx);
        }
    }

    private static class OverrideContext {

        final ImmutableProp prop;

        final int depth;

        final PropOverride annotation;

        private OverrideContext(ImmutableProp prop, int depth, PropOverride annotation) {
            this.prop = prop;
            this.depth = depth;
            this.annotation = annotation;
        }
    }

    private static class CollectContext {

        private final ImmutableProp prop;

        private final MetadataStrategy strategy;

        private final Map<String, String> identifierPathMap = new LinkedHashMap<>();

        private final Map<String, EmbeddedColumns.PathData> pathMap = new LinkedHashMap<>();

        private CollectContext(ImmutableProp prop, MetadataStrategy strategy) {
            this.prop = prop;
            this.strategy = strategy;
        }

        public void accept(EmbeddedTree tree) {
            if (tree.childMap.isEmpty() && !tree.prop.isFormula()) {
                String columnName = null;
                if (tree.usedCtx != null) {
                    columnName = tree.usedCtx.annotation.columnName();
                }
                if (columnName == null) {
                    columnName = userDefinedColumnName(tree.prop);
                }
                columnName = columnName == null ?
                        strategy.getNamingStrategy().columnName(tree.prop) :
                        Utils.resolveMetaString(columnName, strategy.getMetaStringResolver());

                String comparableIdentifier = DatabaseIdentifiers.comparableIdentifier(columnName);
                String conflictPath = identifierPathMap.put(comparableIdentifier, tree.path);
                if (conflictPath != null) {
                    throw new ModelException(
                            "The property \"" +
                                    prop +
                                    "\" is illegal, its an embedded property but " +
                                    "both the path `" +
                                    conflictPath +
                                    "` and `" +
                                    tree.path +
                                    "` has been mapped to an same column \"" +
                                    columnName +
                                    "\""
                    );
                }
                for (EmbeddedTree t = tree; t != null; t = t.parent) {
                    boolean isTerminal = tree == t;
                    pathMap.computeIfAbsent(t.path, it -> new EmbeddedColumns.PathData(isTerminal)).columnNames.add(columnName);
                }
            }
        }

        public EmbeddedColumns toEmbeddedColumns(ImmutableType type) {
            return new EmbeddedColumns(pathMap, type);
        }
    }

    private static String userDefinedColumnName(ImmutableProp prop) {
        Column column = prop.getAnnotation(Column.class);
        if (column == null) {
            return null;
        }
        if (prop.getTargetType() != null && prop.getTargetType().isEmbeddable()) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it cannot be decorated by \"@" +
                            Column.class.getName() +
                            "\" because it is embedded property"
            );
        }
        String value = column.name();
        return value.isEmpty() ? null : column.name();
    }
}
