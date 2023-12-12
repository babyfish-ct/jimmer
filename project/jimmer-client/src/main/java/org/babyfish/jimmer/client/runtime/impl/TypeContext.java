package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.client.runtime.TypeVariable;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

class TypeContext {

    private final Map<TypeName, TypeDefinition> definitionMap;

    private final boolean isGenericSupported;

    private final Map<TypeName, Class<?>> javaTypeMap = new HashMap<>();

    private final Map<ImmutableKey, ImmutableObjectTypeImpl> immutableTypeMap = new LinkedHashMap<>();

    private final Map<StaticKey, StaticObjectTypeImpl> staticTypeMap = new LinkedHashMap<>();

    private final Map<TypeName, EnumTypeImpl> enumTypeMap = new TreeMap<>();

    private GenericReplace genericReplace;

    public TypeContext(
            Map<TypeName, TypeDefinition> definitionMap,
            boolean isGenericSupported
    ) {
        this.definitionMap = definitionMap;
        this.isGenericSupported = isGenericSupported;
    }

    Collection<ImmutableObjectTypeImpl> immutableObjectTypes() {
        return Collections.unmodifiableCollection(immutableTypeMap.values());
    }

    Collection<StaticObjectTypeImpl> staticObjectTypes() {
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
            return immutableObjectType(new ImmutableKey(typeName, typeRef.getFetchBy(), typeRef.getFetcherOwner()));
        }
        if (definition != null && definition.getKind() == TypeDefinition.Kind.ENUM) {
            return enumTypeMap.computeIfAbsent(typeName, it -> new EnumTypeImpl(javaType(it)));
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
                if (isGenericSupported) {
                    return staticObjectType(new StaticKey(typeName, Collections.emptyList()));
                }
                return staticObjectType(new StaticKey(typeName, typeRef.getArguments().stream().map(this::parseType).collect(Collectors.toList())));
        }
    }

    Class<?> javaType(TypeName typeName) {
        return javaTypeMap.computeIfAbsent(typeName, it -> {
            try {
                return Class.forName(typeName.toString());
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void generic(Class<?> rawType, List<Type> arguments, Runnable block) {
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

    private ObjectType immutableObjectType(ImmutableKey key) {
        ImmutableObjectTypeImpl objectType = immutableTypeMap.get(key);
        if (objectType != null) {
            return objectType;
        }
        objectType = new ImmutableObjectTypeImpl(
                ImmutableType.get(javaType(key.typeName))
        );
        immutableTypeMap.put(key, objectType);
        objectType.init(key.fetchBy, key.ownerType, this);
        objectType.setDoc(definition(key.typeName).getDoc());
        return objectType;
    }

    private ObjectType staticObjectType(StaticKey key) {
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

    private static class ImmutableKey {

        final TypeName typeName;

        @Nullable
        final String fetchBy;

        @Nullable
        final TypeName ownerType;

        private ImmutableKey(TypeName typeName, @Nullable String fetchBy, @Nullable TypeName ownerType) {
            this.typeName = typeName;
            this.fetchBy = fetchBy;
            this.ownerType = ownerType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImmutableKey that = (ImmutableKey) o;

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
