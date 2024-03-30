package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.*;

import java.util.*;

public class MetadataImpl implements Metadata {

    private final boolean isGenericSupported;

    private final Map<String, List<Operation>> pathMap;

    private final List<Service> services;
    
    private final List<ObjectType> fetchedTypes;
    
    private final List<ObjectType> dynamicTypes;

    private final List<ObjectType> embeddableTypes;
    
    private final List<ObjectType> staticTypes;
    
    private final List<EnumType> enumTypes;

    private final Map<Class<?>, Type> typeMap;

    public MetadataImpl(
            boolean isGenericSupported,
            List<Service> services,
            List<ObjectType> fetchedTypes,
            List<ObjectType> dynamicTypes,
            List<ObjectType> embeddableTypes,
            List<ObjectType> staticTypes,
            List<EnumType> enumTypes
    ) {
        this.isGenericSupported = isGenericSupported;
        this.pathMap = getPathMap(services);
        this.services = services;
        this.fetchedTypes = fetchedTypes;
        this.dynamicTypes = dynamicTypes;
        this.embeddableTypes = embeddableTypes;
        this.staticTypes = staticTypes;
        this.enumTypes = enumTypes;
        Map<Class<?>, Type> typeMap = new LinkedHashMap<>();
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

    private static Map<String, List<Operation>> getPathMap(List<Service> services) {
        Map<String, Map<Operation.HttpMethod, Operation>> map = new TreeMap<>();
        for (Service service : services) {
            for (Operation operation : service.getOperations()) {
                Map<Operation.HttpMethod, Operation> subMap =
                        map.computeIfAbsent(operation.getUri(), it ->new TreeMap<>());
                for (Operation.HttpMethod method : operation.getHttpMethods()) {
                    Operation conflictOperation = subMap.put(method, operation);
                    if (conflictOperation != null) {
                        throw new IllegalApiException(
                                "Conflict HTTP endpoint \"" +
                                        method +
                                        ":" +
                                        operation.getUri() +
                                        "\" which is shared by \"" +
                                        conflictOperation.getJavaMethod() +
                                        "\" and \"" +
                                        operation.getJavaMethod() +
                                        "\""
                        );
                    }
                }
            }
        }
        Map<String, List<Operation>> pathMap = new TreeMap<>();
        for (Map.Entry<String, Map<Operation.HttpMethod, Operation>> e : map.entrySet()) {
            pathMap.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue().values())));
        }
        return Collections.unmodifiableMap(pathMap);
    }

    @Override
    public boolean isGenericSupported() {
        return isGenericSupported;
    }

    @Override
    public Map<String, List<Operation>> getPathMap() {
        return pathMap;
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
    public List<ObjectType> getEmbeddableTypes() {
        return embeddableTypes;
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
