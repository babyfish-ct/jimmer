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

    private final List<List<ImmutableProp>> insertablePaths;

    private final List<List<ImmutableProp>> updatablePaths;

    private UpsertMask(ImmutableType type) {
        this.type = type;
        this.insertablePaths = null;
        this.updatablePaths = null;
    }

    public UpsertMask(
            ImmutableType type,
            List<List<ImmutableProp>> insertablePaths,
            List<List<ImmutableProp>> updatablePaths
    ) {
        this.type = type;
        this.insertablePaths = insertablePaths;
        this.updatablePaths = updatablePaths;
    }

    private UpsertMask(
            UpsertMask<E> base,
            List<ImmutableProp> insertablePath,
            List<ImmutableProp> updatablePath
    ) {
        this.type = base.type;
        this.insertablePaths = addPath(base.insertablePaths, insertablePath);
        this.updatablePaths = addPath(base.updatablePaths, updatablePath);
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
    public UpsertMask<E> addInsertableProp(ImmutableProp prop) {
        return addInsertablePath(prop);
    }

    @NewChain
    public UpsertMask<E> addInsertableProp(TypedProp.Single<E, ?> prop) {
        return addInsertablePath(prop);
    }

    /**
     * Add insertable path, the type of first property must be embeddable
     * @param props The properties of embeddable properties path
     * @return Another UpsertMask object which is not current object
     */
    @NewChain
    public UpsertMask<E> addInsertablePath(ImmutableProp ... props) {
        return addInsertablePath0(new ArrayList<>(Arrays.asList(props)));
    }

    /**
     * Add insertable path, the first property must be embeddable
     * @param prop The entity property whose type is embeddable
     * @param embeddedProps Deeper properties of embeddable path
     * @return Another UpsertMask object which is not current object
     */
    @NewChain
    public UpsertMask<E> addInsertablePath(TypedProp.Single<E, ?> prop, TypedProp.Single<?, ?> ... embeddedProps) {
        return addInsertablePath0(toList(prop, embeddedProps));
    }

    @NewChain
    public UpsertMask<E> addUpdatableProp(ImmutableProp prop) {
        return addUpdatablePath(prop);
    }

    @NewChain
    public UpsertMask<E> addUpdatableProp(TypedProp.Single<E, ?> prop) {
        return addUpdatablePath(prop);
    }

    /**
     * Add updatable path, the type of first property must be embeddable
     * @param props The properties of embeddable properties path
     * @return Another UpsertMask object which is not current object
     */
    @NewChain
    public UpsertMask<E> addUpdatablePath(ImmutableProp ... props) {
        return addUpdatablePath0(new ArrayList<>(Arrays.asList(props)));
    }

    /**
     * Add updatable path, the first property must be embeddable
     * @param prop The entity property whose type is embeddable
     * @param embeddedProps Deeper properties of embeddable path
     * @return Another UpsertMask object which is not current object
     */
    @NewChain
    public UpsertMask<E> addUpdatablePath(TypedProp.Single<E, ?> prop, TypedProp.Single<?, ?> ... embeddedProps) {
        return addUpdatablePath0(toList(prop, embeddedProps));
    }

    @NewChain
    public UpsertMask<E> forbidInsert() {
        if (insertablePaths != null && insertablePaths.isEmpty()) {
            return this;
        }
        return new UpsertMask<>(type, Collections.emptyList(), updatablePaths);
    }

    @NewChain
    public UpsertMask<E> forbidUpdate() {
        if (updatablePaths != null && updatablePaths.isEmpty()) {
            return this;
        }
        return new UpsertMask<>(type, insertablePaths, Collections.emptyList());
    }

    @NotNull
    public ImmutableType getType() {
        return type;
    }

    @Nullable
    public List<List<ImmutableProp>> getInsertablePaths() {
        return insertablePaths;
    }

    @Nullable
    public List<List<ImmutableProp>> getUpdatablePaths() {
        return updatablePaths;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UpsertMask {entityType = \"").append(type).append("\"");
        if (insertablePaths != null) {
            builder.append(", insertablePaths=[");
            appendPaths(builder, insertablePaths);
            builder.append(']');
        }
        if (updatablePaths != null) {
            builder.append(", updatablePaths=[");
            appendPaths(builder, updatablePaths);
            builder.append(']');
        }
        builder.append("}");
        return builder.toString();
    }

    private UpsertMask<E> addInsertablePath0(List<ImmutableProp> props) {
        validateProps(props);
        return new UpsertMask<>(this, props, null);
    }

    private UpsertMask<E> addUpdatablePath0(List<ImmutableProp> props) {
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
