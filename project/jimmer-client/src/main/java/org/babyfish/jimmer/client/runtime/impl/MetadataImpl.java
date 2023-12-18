package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.*;

import java.util.*;

public class MetadataImpl implements Metadata {

    private final boolean isGenericSupported;

    private final List<Service> services;
    
    private final List<ObjectType> fetchedTypes;
    
    private final List<ObjectType> dynamicTypes;
    
    private final List<ObjectType> staticTypes;
    
    private final List<EnumType> enumTypes;

    private final Map<Class<?>, Type> typeMap;

    public MetadataImpl(
            boolean isGenericSupported,
            List<Service> services,
            List<ObjectType> fetchedTypes,
            List<ObjectType> dynamicTypes,
            List<ObjectType> staticTypes,
            List<EnumType> enumTypes
    ) {
        this.isGenericSupported = isGenericSupported;
        this.services = services;
        this.fetchedTypes = fetchedTypes;
        this.dynamicTypes = dynamicTypes;
        this.staticTypes = staticTypes;
        this.enumTypes = enumTypes;
        Map<Class<?>, Type> typeMap = new HashMap();
        for (ObjectType fetchedType : fetchedTypes) {
            typeMap.put(fetchedType.getJavaType(), fetchedType);
        }
        for (ObjectType dynamicType : dynamicTypes) {
            typeMap.put(dynamicType.getJavaType(), dynamicType);
        }
        for (ObjectType staticType : staticTypes) {
            typeMap.put(staticType.getJavaType(), staticType);
        }
        for (EnumType enumType : enumTypes) {
            typeMap.put(enumType.getJavaType(), enumType);
        }
        this.typeMap = typeMap;
    }

    @Override
    public boolean isGenericSupported() {
        return isGenericSupported;
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
    public Type getType(Class<?> type) {
        return typeMap.get(type);
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
