package org.babyfish.jimmer.sql.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MutationPath {

    private final ImmutableType type;

    private final ImmutableProp prop;

    private final ImmutableProp backProp;

    private final MutationPath parent;

    private MutationPath(ImmutableType type) {
        this.type = Objects.requireNonNull(type, "`type` cannot be null");
        this.prop = null;
        this.backProp = null;
        this.parent = null;
    }

    private MutationPath(MutationPath parent, ImmutableProp prop, ImmutableProp backProp) {
        if (prop != null && backProp != null) {
            throw new IllegalArgumentException(
                    "\"prop\" and \"backReferenceProp\" cannot be specified at the same time"
            );
        }
        this.parent = parent;
        if (prop != null) {
            if (!prop.isAssociation(TargetLevel.ENTITY) || prop.isTransient()) {
                throw new IllegalArgumentException("The property \"" + prop + "\" is not association property");
            }
            if (!prop.getDeclaringType().isAssignableFrom(parent.type)) {
                throw new IllegalArgumentException(
                        "The declaring type of property \"" +
                                prop +
                                "\" is not assignable from \"" +
                                getType() +
                                "\""
                );
            }
            this.type = prop.getTargetType();
            this.prop = prop;
            this.backProp = prop.getOpposite();
        } else {
            if (!backProp.isAssociation(TargetLevel.ENTITY) || backProp.isTransient()) {
                throw new IllegalArgumentException(
                        "The back reference property \"" +
                                backProp +
                                "\" is not association property"
                );
            }
            if (!backProp.getTargetType().isAssignableFrom(parent.type)) {
                throw new IllegalArgumentException(
                        "The target type of back reference property \"" +
                                backProp +
                                "\" is not assignable from \"" +
                                getType() +
                                "\""
                );
            }
            this.type = backProp.getDeclaringType();
            this.prop = backProp.getOpposite();
            this.backProp = backProp;
        }
    }

    public static MutationPath root(ImmutableType type) {
        return new MutationPath(type);
    }

    public MutationPath to(ImmutableProp prop) {
        return new MutationPath(this, prop, null);
    }

    public MutationPath backFrom(ImmutableProp backReferenceProp) {
        ImmutableProp prop = backReferenceProp.getOpposite();
        if (prop != null) {
            return to(prop);
        }
        return new MutationPath(this, null, backReferenceProp);
    }

    @JsonIgnore
    public ImmutableType getType() {
        return type;
    }

    public String getTypeName() {
        return type.toString();
    }

    public MutationPath getParent() {
        return parent;
    }

    @JsonIgnore
    public ImmutableProp getProp() {
        return prop;
    }

    @JsonIgnore
    public ImmutableProp getBackProp() {
        return backProp;
    }

    public boolean contains(ImmutableProp prop) {
        if (this.prop == prop) {
            return true;
        }
        if (parent != null) {
            return parent.contains(prop);
        }
        return false;
    }

    public boolean contains(TypedProp.Association<?, ?> prop) {
        return contains(prop.unwrap());
    }

    public boolean contains(ImmutableType type) {
        if (this.type == type) {
            return true;
        }
        if (parent != null) {
            return parent.contains(type);
        }
        return false;
    }

    public boolean contains(Class<?> type) {
        return contains(ImmutableType.get(type));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, prop, backProp, parent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutationPath other = (MutationPath) o;
        return type == other.type &&
                Objects.equals(prop, other.prop) &&
                Objects.equals(backProp, other.backProp) &&
                Objects.equals(parent, other.parent);
    }

    @Override
    public String toString() {
        if (parent == null) {
            return "<root>";
        }
        if (prop != null) {
            return parent + "." + prop.getName();
        }
        return parent + ".[←" + backProp.getName() + ']';
    }

    public ExportedSavePath export() {
        List<MutationPath> paths = new ArrayList<>();
        for (MutationPath p = this; p != null; p = p.parent) {
            paths.add(p);
        }
        Collections.reverse(paths);
        int size = paths.size();
        List<ExportedSavePath.Node> nodes = new ArrayList<>(size - 1);
        for (int i = 1; i < size; i++) {
            MutationPath path = paths.get(i);
            if (path.prop != null) {
                nodes.add(
                        new ExportedSavePath.Node(
                                path.prop.getName(),
                                path.prop.getTargetType().toString()
                        )
                );
            } else {
                nodes.add(
                        new ExportedSavePath.Node(
                                "[←" + backProp.getName() + ']',
                                backProp.getDeclaringType().toString()
                        )
                );
            }
        }
        return new ExportedSavePath(paths.get(0).type.toString(), nodes);
    }

    public static MutationPath of(ExportedSavePath path) {
        Class<?> rootType;
        try {
            rootType = Class.forName(path.getRootTypeName());
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(
                    "The root type \"" +
                            path.getRootTypeName() +
                            "\" does not exist"
            );
        }
        return of(path, rootType);
    }

    public static MutationPath of(ExportedSavePath path, ClassLoader classLoader) {
        Class<?> rootType;
        try {
            rootType = Class.forName(path.getRootTypeName(), true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(
                    "The root type \"" +
                            path.getRootTypeName() +
                            "\" does not exist"
            );
        }
        return of(path, rootType);
    }

    private static MutationPath of(ExportedSavePath path, Class<?> rootType) {
        MutationPath mutationPath = new MutationPath(ImmutableType.get(rootType));
        for (ExportedSavePath.Node node : path.getNodes()) {
            ImmutableProp prop = mutationPath.getType().getProp(node.getProp());
            mutationPath = mutationPath.to(prop);
        }
        return mutationPath;
    }

}
