package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicTypeImpl implements ObjectType {

    private final ImmutableType immutableType;

    private Map<String, Property> properties;

    private Doc doc;

    public DynamicTypeImpl(ImmutableType immutableType) {
        this.immutableType = immutableType;
    }

    void init(TypeName typeName, TypeContext ctx) {
        try {
            TypeDefinition definition = ctx.definition(typeName);
            this.doc = definition.getDoc();
            Map<String, Property> properties = new LinkedHashMap<>((definition.getPropMap().size() * 4 + 2) / 3);
            for (Prop prop : definition.getPropMap().values()) {
                try {
                    Property property = new PropertyImpl(
                            prop.getName(),
                            ctx.parseType(prop.getType()),
                            prop.getDoc()
                    );
                    properties.put(property.getName(), property);
                } catch (Throwable ex) {
                    throw new TypeResolvingException(typeName, '@' + prop.getName(), ex);
                }
            }
            this.properties = Collections.unmodifiableMap(properties);
        } catch (TypeResolvingException ex) {
            throw  ex;
        } catch (Throwable ex) {
            throw new TypeResolvingException(typeName, ex);
        }
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
        return Kind.DYNAMIC;
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
}
