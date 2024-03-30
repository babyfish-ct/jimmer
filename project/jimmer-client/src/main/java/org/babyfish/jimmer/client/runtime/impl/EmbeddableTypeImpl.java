package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.FetchByInfo;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class EmbeddableTypeImpl extends Graph implements ObjectType {

    private final ImmutableType immutableType;

    private Map<String, Property> properties;

    private Doc doc;

    public EmbeddableTypeImpl(ImmutableType immutableType) {
        this.immutableType = immutableType;
    }

    void init(TypeName typeName, TypeContext ctx) {
        TypeDefinition definition = ctx.definition(typeName);
        this.doc = definition.getDoc();
        Map<String, Property> properties = new LinkedHashMap<>((definition.getPropMap().size() * 4 + 2) / 3);
        for (Prop prop : definition.getPropMap().values()) {
            Property property = new PropertyImpl(prop.getName(), ctx.parseType(prop.getType()), prop.getDoc());
            properties.put(property.getName(), property);
        }
        this.properties = Collections.unmodifiableMap(properties);
    }

    @Override
    public Class<?> getJavaType() {
        return immutableType.getJavaClass();
    }

    @Nullable
    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public Kind getKind() {
        return Kind.EMBEDDABLE;
    }

    @Override
    public List<String> getSimpleNames() {
        return Collections.singletonList(getJavaType().getSimpleName());
    }

    @Nullable
    @Override
    public FetchByInfo getFetchByInfo() {
        return null;
    }

    @Override
    public List<Type> getArguments() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    @Nullable
    @Override
    public TypeDefinition.Error getError() {
        return null;
    }

    @Override
    public Map<String, Property> getProperties() {
        return properties;
    }

    @Override
    public boolean isRecursiveFetchedType() {
        return false;
    }

    @Override
    public boolean hasMultipleRecursiveProps() {
        return false;
    }

    @Override
    public ObjectType unwrap() {
        return null;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return immutableType +
                " {" +
                properties.values().stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                '}';
    }
}
