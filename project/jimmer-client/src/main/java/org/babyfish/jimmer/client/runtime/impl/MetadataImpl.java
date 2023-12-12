package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.EnumType;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Service;

import java.util.List;

public class MetadataImpl implements Metadata {

    private final List<Service> services;
    
    private final List<ObjectType> fetchedTypes;
    
    private final List<ObjectType> dynamicTypes;
    
    private final List<ObjectType> staticTypes;
    
    private final List<EnumType> enumTypes;

    public MetadataImpl(
            List<Service> services, 
            List<ObjectType> fetchedTypes, 
            List<ObjectType> dynamicTypes, 
            List<ObjectType> staticTypes, 
            List<EnumType> enumTypes
    ) {
        this.services = services;
        this.fetchedTypes = fetchedTypes;
        this.dynamicTypes = dynamicTypes;
        this.staticTypes = staticTypes;
        this.enumTypes = enumTypes;
    }

    @Override
    public List<Service> getServices() {
        return services;
    }

    @Override
    public List<ObjectType> getFetchedTypes() {
        return fetchedTypes;
    }

    @Override
    public List<ObjectType> getDynamicTypes() {
        return dynamicTypes;
    }

    @Override
    public List<ObjectType> getStaticTypes() {
        return staticTypes;
    }

    @Override
    public List<EnumType> getEnumTypes() {
        return enumTypes;
    }

    @Override
    public String toString() {
        return "MetadataImpl{" +
                "services=" + services +
                ", fetchedTypes=" + fetchedTypes +
                ", dynamicTypes=" + dynamicTypes +
                ", staticTypes=" + staticTypes +
                ", enumTypes=" + enumTypes +
                '}';
    }
}
