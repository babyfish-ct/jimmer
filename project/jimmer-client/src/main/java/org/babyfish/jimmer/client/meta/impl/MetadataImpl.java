package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Type;
import java.util.Map;

public class MetadataImpl implements Metadata {

    private final Map<Class<?>, Service> services;

    private final Map<Type, ObjectType> staticTypes;

    private final Map<Class<?>, EnumType> enumTypes;

    private final Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypes;

    private final Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypes;

    public MetadataImpl(
            Map<Class<?>, Service> services,
            Map<Type, ObjectType> staticTypes,
            Map<Class<?>, EnumType> enumTypes,
            Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypes,
            Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypes
    ) {
        this.services = services;
        this.staticTypes = staticTypes;
        this.enumTypes = enumTypes;
        this.viewImmutableObjectTypes = viewImmutableObjectTypes;
        this.rawImmutableObjectTypes = rawImmutableObjectTypes;
    }

    @Override
    public Map<Class<?>, Service> getServices() {
        return services;
    }

    @Override
    public Map<Type, ObjectType> getStaticTypes() {
        return staticTypes;
    }

    @Override
    public Map<Class<?>, EnumType> getEnumTypes() {
        return enumTypes;
    }

    @Override
    public Map<ImmutableType, ImmutableObjectType> getViewImmutableObjectTypes() {
        return viewImmutableObjectTypes;
    }

    @Override
    public Map<ImmutableType, ImmutableObjectType> getRawImmutableObjectTypes() {
        return rawImmutableObjectTypes;
    }

    @Override
    public String toString() {
        return "MetadataImpl{" +
                "services=" + services +
                ", staticTypes=" + staticTypes +
                ", enumTypes=" + enumTypes +
                ", viewImmutableObjectTypes=" + viewImmutableObjectTypes +
                ", rawImmutableObjectTypes=" + rawImmutableObjectTypes +
                '}';
    }
}
