package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.client.runtime.TypeVariable;
import org.babyfish.jimmer.client.runtime.VirtualType;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Embeddable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

class TypeContext {

    private final Map<TypeName, TypeDefinition> definitionMap;

    private final Map<TypeName, VirtualType> virtualTypeMap;

    private final boolean isGenericSupported;

    private final Map<TypeName, Class<?>> javaTypeMap = new HashMap<>();

    private final Map<FetchedKey, FetchedTypeImpl> fetchedTypeMap = new LinkedHashMap<>();

    private final Map<TypeName, DynamicTypeImpl> dynamicTypeMap = new LinkedHashMap<>();

    private final Map<TypeName, EmbeddableTypeImpl> embeddableTypeMap = new LinkedHashMap<>();

    private final Map<StaticKey, StaticObjectTypeImpl> staticTypeMap = new LinkedHashMap<>();

    private final Map<TypeName, EnumTypeImpl> enumTypeMap = new TreeMap<>();

    private final Map<TypeDefinition.Error, StaticObjectTypeImpl> errorTypeMap = new HashMap<>();

    private GenericReplace genericReplace;

    public TypeContext(
            Map<TypeName, TypeDefinition> definitionMap,
            Map<TypeName, VirtualType> virtualTypeMap,
            boolean isGenericSupported
    ) {
        this.definitionMap = definitionMap;
        this.virtualTypeMap = virtualTypeMap;
        this.isGenericSupported = isGenericSupported;
    }

    Collection<FetchedTypeImpl> fetchedTypes() {
        return Collections.unmodifiableCollection(fetchedTypeMap.values());
    }

    Collection<DynamicTypeImpl> dynamicTypes() {
        return Collections.unmodifiableCollection(dynamicTypeMap.values());
    }

    Collection<EmbeddableTypeImpl> embeddableTypes() {
        return Collections.unmodifiableCollection(embeddableTypeMap.values());
    }

    Collection<StaticObjectTypeImpl> staticTypes() {
        return Collections.unmodifiableCollection(staticTypeMap.values());
    }

    Collection<EnumTypeImpl> enumTypes() {
        return Collections.unmodifiableCollection(enumTypeMap.values());
    }

    TypeDefinition definition(Class<?> type) {
        return definitionMap.get(TypeName.of(type));
    }

    TypeDefinition definition(TypeName typeName) {
        TypeDefinition definition = definitionMap.get(typeName);
        if (definition == null) {
            throw new IllegalApiException(
                    "No type definition for \"" +
                            typeName +
                            "\"");
        }
        return definition;
    }

    Type parseType(TypeRef typeRef) {
        if (typeRef.isNullable()) {
            return NullableTypeImpl.of(parseNonNullType(typeRef));
        }
        return parseNonNullType(typeRef);
    }

    private Type parseNonNullType(TypeRef typeRef) {
        TypeName typeName = typeRef.getTypeName();
        VirtualType virtualType = virtualTypeMap.get(typeName);
        if (virtualType != null) {
            return virtualType;
        }
        if (typeName.getTypeVariable() != null) {
            if (genericReplace == null) {
                return new TypeVariableImpl(typeName);
            }
            return genericReplace.resolve(typeName);
        }
        if (!typeName.isGenerationRequired() && typeRef.getArguments().isEmpty()) {
            return SimpleTypeImpl.of(typeName);
        }
        TypeDefinition definition = definitionMap.get(typeName);
        if (definition != null && definition.getKind() == TypeDefinition.Kind.IMMUTABLE) {
            if (typeRef.getFetchBy() == null) {
                Class<?> javaType = javaType(typeName);
                return javaType.isAnnotationPresent(Embeddable.class) ?
                        embeddableType(typeName) :
                        dynamicType(typeName);
            }
            return fetchedType(new FetchedKey(typeName, typeRef.getFetchBy(), typeRef.getFetcherOwner()), typeRef.getFetcherDoc());
        }
        if (definition != null && definition.getKind() == TypeDefinition.Kind.ENUM) {
            return enumTypeMap.computeIfAbsent(typeName, it -> new EnumTypeImpl(javaType(it), definition(it)));
        }
        switch (typeName.toString()) {
            case "java.util.List":
                return new ListTypeImpl(parseType(typeRef.getArguments().get(0)));
            case "java.util.Map":
                return new MapTypeImpl(
                        parseType(typeRef.getArguments().get(0)),
                        parseType(typeRef.getArguments().get(1))
                );
            default:
                List<Type> arguments = typeRef.getArguments().stream().map(this::parseType).collect(Collectors.toList());
                if (isGenericSupported && !arguments.isEmpty()) {
                    StaticObjectTypeImpl raw = staticType(new StaticKey(typeName, Collections.emptyList()));
                    return new GenericTypeImpl(raw, arguments);
                }
                return staticType(new StaticKey(typeName, arguments));
        }
    }

    Class<?> javaType(TypeName typeName) {
        return javaTypeMap.computeIfAbsent(typeName, it -> {
            try {
                return Class.forName(typeName.toString(true));
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    void generic(Class<?> rawType, List<Type> arguments, Runnable block) {
        if (arguments.isEmpty()) {
            block.run();
            return;
        }
        java.lang.reflect.TypeVariable<?>[] arr = rawType.getTypeParameters();
        GenericReplace genericReplace = new GenericReplace(this.genericReplace);
        for (int i = 0; i < arr.length; i++) {
            TypeName typeName = TypeName.of(rawType).typeVariable(arr[i].getName());
            genericReplace.map.put(typeName, arguments.get(i));
        }
        this.genericReplace = genericReplace;
        try {
            block.run();
        } finally {
            this.genericReplace = genericReplace.parent;
        }
    }

    private FetchedTypeImpl fetchedType(FetchedKey key, Doc fetcherDoc) {
        FetchedTypeImpl objectType = fetchedTypeMap.get(key);
        if (objectType != null) {
            return objectType;
        }
        objectType = new FetchedTypeImpl(
                ImmutableType.get(javaType(key.typeName))
        );
        fetchedTypeMap.put(key, objectType);
        objectType.init(key.fetchBy, key.ownerType, fetcherDoc, this);
        return objectType;
    }

    private DynamicTypeImpl dynamicType(TypeName typeName) {
        DynamicTypeImpl objectType = dynamicTypeMap.get(typeName);
        if (objectType != null) {
            return objectType;
        }
        DynamicTypeImpl newObjectType = new DynamicTypeImpl(
                ImmutableType.get(javaType(typeName))
        );
        dynamicTypeMap.put(typeName, newObjectType);
        newObjectType.init(typeName, this);
        return newObjectType;
    }

    private EmbeddableTypeImpl embeddableType(TypeName typeName) {
        EmbeddableTypeImpl objectType = embeddableTypeMap.get(typeName);
        if (objectType != null) {
            return objectType;
        }
        EmbeddableTypeImpl newObjectType = new EmbeddableTypeImpl(
                ImmutableType.get(javaType(typeName))
        );
        embeddableTypeMap.put(typeName, newObjectType);
        newObjectType.init(typeName, this);
        return newObjectType;
    }

    private StaticObjectTypeImpl staticType(StaticKey key) {
        StaticObjectTypeImpl objectType = staticTypeMap.get(key);
        if (objectType != null) {
            return objectType;
        }
        StaticObjectTypeImpl newObjectType = new StaticObjectTypeImpl(
                javaType(key.typeName)
        );
        staticTypeMap.put(key, newObjectType);
        this.generic(javaType(key.typeName), key.arguments, () -> {
            newObjectType.init(key.typeName, key.arguments, this);
        });
        if (newObjectType.getError() != null && !newObjectType.getError().getCode().isEmpty()) {
            StaticObjectTypeImpl conflictType = errorTypeMap.put(newObjectType.getError(), newObjectType);
            if (conflictType != null) {
                throw new IllegalApiException(
                        "Conflict exceptions, the error family \"" +
                                newObjectType.getError().getFamily() +
                                "\" and code \"" +
                                newObjectType.getError().getCode() +
                                "\" are shared by \"" +
                                newObjectType.getJavaType().getName() +
                                "\" and \"" +
                                conflictType.getJavaType().getTypeName() +
                                "\""
                );
            }
        }
        return newObjectType;
    }

    private static class GenericReplace {

        final GenericReplace parent;

        final Map<TypeName, Type> map = new HashMap<>();

        GenericReplace(GenericReplace parent) {
            this.parent = parent;
        }

        Type resolve(TypeName typeName) {
            Type type = map.get(typeName);
            if (type instanceof TypeVariable && parent != null) {
                parent.resolve(((TypeVariable)type).getTypeName());
            }
            if (type != null) {
                return type;
            }
            return new TypeVariableImpl(typeName);
        }
    }

    private static class FetchedKey {

        final TypeName typeName;

        @Nullable
        final String fetchBy;

        @Nullable
        final TypeName ownerType;

        private FetchedKey(TypeName typeName, @Nullable String fetchBy, @Nullable TypeName ownerType) {
            this.typeName = typeName;
            this.fetchBy = fetchBy;
            this.ownerType = ownerType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FetchedKey that = (FetchedKey) o;

            if (!typeName.equals(that.typeName)) return false;
            if (!Objects.equals(fetchBy, that.fetchBy)) return false;
            return Objects.equals(ownerType, that.ownerType);
        }

        @Override
        public int hashCode() {
            int result = typeName.hashCode();
            result = 31 * result + (fetchBy != null ? fetchBy.hashCode() : 0);
            result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ImmutableType{" +
                    "typeName=" + typeName +
                    ", fetchBy='" + fetchBy + '\'' +
                    ", ownerType=" + ownerType +
                    '}';
        }
    }

    private static class StaticKey {

        final TypeName typeName;

        final List<Type> arguments;

        private StaticKey(TypeName typeName, List<Type> arguments) {
            this.typeName = typeName;
            this.arguments = arguments;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StaticKey key = (StaticKey) o;

            if (!typeName.equals(key.typeName)) return false;
            return arguments.equals(key.arguments);
        }

        @Override
        public int hashCode() {
            int result = typeName.hashCode();
            result = 31 * result + arguments.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "typeName=" + typeName +
                    ", arguments=" + arguments +
                    '}';
        }
    }
}
