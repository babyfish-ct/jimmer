package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.runtime.FetchByInfo;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenericObjectTypeImpl extends Graph implements ObjectType {

    private final StaticObjectTypeImpl raw;

    private final List<Type> arguments;

    public GenericObjectTypeImpl(StaticObjectTypeImpl raw, List<Type> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("argument cannot be empty");
        }
        this.raw = raw;
        this.arguments = arguments;
    }

    @Override
    public Class<?> getJavaType() {
        return raw.getJavaType();
    }

    @Nullable
    @Override
    public ImmutableType getImmutableType() {
        return raw.getImmutableType();
    }

    @Override
    public Kind getKind() {
        return Kind.STATIC;
    }

    @Override
    public List<String> getSimpleNames() {
        return raw.getSimpleNames();
    }

    @Nullable
    @Override
    public FetchByInfo getFetchByInfo() {
        return raw.getFetchByInfo();
    }

    @Override
    public List<Type> getArguments() {
        return arguments;
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return raw.getDoc();
    }

    @Nullable
    @Override
    public TypeDefinition.Error getError() {
        return raw.getError();
    }

    @Override
    public Map<String, Property> getProperties() {
        return raw.getProperties();
    }

    @Override
    public boolean isRecursiveFetchedType() {
        return false;
    }

    @Override
    public ObjectType unwrap() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericObjectTypeImpl that = (GenericObjectTypeImpl) o;

        if (!raw.equals(that.raw)) return false;
        return arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        int result = raw.hashCode();
        result = 31 * result + arguments.hashCode();
        return result;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        Class<?> javaType = getJavaType();
        Map<String, Property> properties = getProperties();
        return javaType.getName() +
                '<' +
                arguments.stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                "> {" +
                properties.values().stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                '}';
    }
}
