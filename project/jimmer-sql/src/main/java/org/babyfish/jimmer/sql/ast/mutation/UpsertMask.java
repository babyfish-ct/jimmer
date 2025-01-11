package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UpsertMask<E> {

    private final ImmutableType type;

    private final List<List<ImmutableProp>> insertedPaths;

    private final List<List<ImmutableProp>> updatedPaths;

    private UpsertMask(ImmutableType type) {
        this.type = type;
        this.insertedPaths = null;
        this.updatedPaths = null;
    }

    private UpsertMask(
            UpsertMask<E> base,
            List<ImmutableProp> insertedPath,
            List<ImmutableProp> updatedPath
    ) {
        this.type = base.type;
        this.insertedPaths = addPath(base.insertedPaths, insertedPath);
        this.updatedPaths = addPath(base.updatedPaths, updatedPath);
    }

    public static <E> UpsertMask<E> of(Class<E> type) {
        ImmutableType immutableType = ImmutableType.get(type);
        if (!immutableType.isEntity()) {
            throw new IllegalArgumentException(
                    "The type \"" + type + "\" is not entity"
            );
        }
        return new UpsertMask<>(immutableType);
    }

    @NewChain
    public UpsertMask<E> addInsertedProp(ImmutableProp prop) {
        return addInsertedPath(prop);
    }

    @NewChain
    public UpsertMask<E> addInsertedProp(TypedProp.Single<E, ?> prop) {
        return addInsertedPath(prop);
    }

    @NewChain
    public UpsertMask<E> addInsertedPath(ImmutableProp ... props) {
        return addInsertedPath0(new ArrayList<>(Arrays.asList(props)));
    }

    @NewChain
    public UpsertMask<E> addInsertedPath(TypedProp.Single<E, ?> prop, TypedProp.Single<?, ?> ... embeddedProps) {
        return addInsertedPath0(toList(prop, embeddedProps));
    }

    @NewChain
    public UpsertMask<E> addUpdatedProp(ImmutableProp prop) {
        return addUpdatedPath(prop);
    }

    @NewChain
    public UpsertMask<E> addUpdatedProp(TypedProp.Single<E, ?> prop) {
        return addUpdatedPath(prop);
    }

    @NewChain
    public UpsertMask<E> addUpdatedPath(ImmutableProp ... props) {
        return addUpdatePath0(new ArrayList<>(Arrays.asList(props)));
    }

    public UpsertMask<E> addUpdatedPath(TypedProp.Single<E, ?> prop, TypedProp.Single<?, ?> ... embeddedProps) {
        return addUpdatePath0(toList(prop, embeddedProps));
    }

    @NotNull
    public ImmutableType getType() {
        return type;
    }

    @Nullable
    public List<List<ImmutableProp>> getInsertedPaths() {
        return insertedPaths;
    }

    @Nullable
    public List<List<ImmutableProp>> getUpdatedPaths() {
        return updatedPaths;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UpsertMask {entityType = \"").append(type).append("\"");
        if (insertedPaths != null) {
            builder.append(", insertedPaths=[");
            appendPaths(builder, insertedPaths);
            builder.append(']');
        }
        if (updatedPaths != null) {
            builder.append(", updatedPaths=[");
            appendPaths(builder, updatedPaths);
            builder.append(']');
        }
        builder.append("}");
        return builder.toString();
    }

    private UpsertMask<E> addInsertedPath0(List<ImmutableProp> props) {
        validateProps(props);
        return new UpsertMask<>(this, props, null);
    }

    private UpsertMask<E> addUpdatePath0(List<ImmutableProp> props) {
        validateProps(props);
        return new UpsertMask<>(this, null, props);
    }

    private void validateProps(List<ImmutableProp> props) {
        ImmutableType type = this.type;
        int len = props.size();
        if (len == 0) {
            throw new IllegalArgumentException("path cannot be empty");
        }
        for (int i = 0; i < len; i++) {
            ImmutableProp prop = props.get(i);
            if (type == null) {
                throw new IllegalArgumentException(
                        "Cannot access the property \"" +
                                prop +
                                "\" based on the existing path \"" +
                                path(props, i) +
                                "\" because the path is not embeddable type"
                );
            }
            if (prop.getDeclaringType() != type) {
                throw new IllegalArgumentException(
                        "Cannot access the property \"" +
                                prop +
                                "\" based on the existing path \"" +
                                path(props, i) +
                                "\", the declaring type must be \"" +
                                type +
                                "\""
                );
            }
            type = prop.getTargetType();
            if (type != null && type.isEntity()) {
                type = type.getIdProp().getTargetType();
            }
        }
    }

    private String path(List<ImmutableProp> props, int end) {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        for (int i = 0; i < end; i++) {
            ImmutableProp prop = props.get(i);
            builder.append("->").append(prop.getName());
            if (prop.isReference(TargetLevel.ENTITY)) {
                builder.append("Id");
            }
        }
        return builder.toString();
    }

    private static List<List<ImmutableProp>> addPath(
            List<List<ImmutableProp>> paths,
            List<ImmutableProp> path
    ) {
        if (path == null) {
            return paths;
        }
        List<List<ImmutableProp>> newPaths = new ArrayList<>(
                (paths != null ? paths.size() : 0) + 1
        );
        if (paths != null) {
            newPaths.addAll(paths);
        }
        newPaths.add(Collections.unmodifiableList(path));
        return Collections.unmodifiableList(newPaths);
    }

    private static List<ImmutableProp> toList(TypedProp.Single<?, ?> prop, TypedProp.Single<?, ?>[] embeddedProps) {
        List<ImmutableProp> props =
                new ArrayList<>(1 + (embeddedProps != null ? embeddedProps.length : 0));
        props.add(prop.unwrap());
        if (embeddedProps != null) {
            for (TypedProp.Single<?, ?> embeddedProp : embeddedProps) {
                props.add(embeddedProp.unwrap());
            }
        }
        return props;
    }

    private void appendPaths(StringBuilder builder, List<List<ImmutableProp>> paths) {
        boolean addComma = false;
        for (List<ImmutableProp> path : paths) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(path(path, path.size()));
        }
    }
}
