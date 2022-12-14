package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Map;

public class MetadataImpl implements Metadata {

    private final Map<Class<?>, Service> services;

    private final Map<Class<?>, StaticObjectType> genericeTypes;

    private final Map<StaticObjectType.Key, StaticObjectType> staticTypes;

    private final Map<Class<?>, EnumType> enumTypes;

    private final Map<Fetcher<?>, ImmutableObjectType> fetchedImmutableObjectTypeMap;

    private final Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypes;

    private final Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypes;

    public MetadataImpl(
            Map<Class<?>, Service> services,
            Map<Class<?>, StaticObjectType> genericTypes,
            Map<StaticObjectType.Key, StaticObjectType> staticTypes,
            Map<Class<?>, EnumType> enumTypes,
            Map<Fetcher<?>, ImmutableObjectType> fetchedImmutableObjectTypeMap,
            Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypes,
            Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypes
    ) {
        this.services = services;
        this.genericeTypes = genericTypes;
        this.staticTypes = staticTypes;
        this.enumTypes = enumTypes;
        this.fetchedImmutableObjectTypeMap = fetchedImmutableObjectTypeMap;
        this.viewImmutableObjectTypes = viewImmutableObjectTypes;
        this.rawImmutableObjectTypes = rawImmutableObjectTypes;
    }

    @Override
    public Map<Class<?>, Service> getServices() {
        return services;
    }

    @Override
    public Map<Class<?>, StaticObjectType> getGenericTypes() {
        return genericeTypes;
    }

    @Override
    public Map<StaticObjectType.Key, StaticObjectType> getStaticTypes() {
        return staticTypes;
    }

    @Override
    public Map<Class<?>, EnumType> getEnumTypes() {
        return enumTypes;
    }

    @Override
    public Map<Fetcher<?>, ImmutableObjectType> getFetchedImmutableObjectTypes() {
        return fetchedImmutableObjectTypeMap;
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
