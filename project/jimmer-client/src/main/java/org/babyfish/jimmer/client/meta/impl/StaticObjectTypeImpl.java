package org.babyfish.jimmer.client.meta.impl;

import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import kotlin.reflect.KType;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.babyfish.jimmer.client.ExportFields;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.Type;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

public class StaticObjectTypeImpl implements StaticObjectType {

    private final StaticObjectTypeImpl declaringObjectType;

    private final Class<?> javaType;

    private final List<Type> typeArguments;

    private NavigableMap<String, Property> props;

    private final Document document;

    private final NavigableMap<String, StaticObjectType> usedNestedJavaTypes = new TreeMap<>();

    StaticObjectTypeImpl(StaticObjectTypeImpl declaringObjectType, Class<?> javaType, List<Type> typeArguments) {
        this.declaringObjectType = declaringObjectType;
        this.javaType = javaType;
        this.typeArguments = typeArguments != null ?
                Collections.unmodifiableList(typeArguments) :
                Collections.emptyList();
        this.document = DocumentImpl.of(javaType);
        if (declaringObjectType != null && (typeArguments == null || typeArguments.isEmpty())) {
            declaringObjectType.usedNestedJavaTypes.put(javaType.getSimpleName(), this);
        }
    }

    @Override
    public StaticObjectTypeImpl getDeclaringObjectType() {
        return declaringObjectType;
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public List<Type> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public Collection<StaticObjectType> getNestedTypes() {
        return Collections.unmodifiableCollection(usedNestedJavaTypes.values());
    }

    @Override
    public boolean isEntity() {
        return false;
    }

    @Override
    public Map<String, Property> getProperties() {
        return props;
    }

    @Nullable
    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            if (visitor.visitStaticObjectType(this)) {
                for (Property prop : props.values()) {
                    prop.getType().accept(visitor);
                }
            }
        }
    }

    @Override
    public String toString() {
        if (typeArguments.isEmpty()) {
            return "@static:" + javaType.getSimpleName();
        }
        return "@static:" + javaType.getSimpleName() + "<...>";
    }

    static StaticObjectType create(Context ctx, Class<?> javaType, List<Type> typeArguments) {

        StaticObjectTypeImpl impl = (StaticObjectTypeImpl) ctx.getStaticObjectType(javaType, typeArguments);
        if (impl != null) {
            return impl;
        }

        StaticObjectType declaringObjectType = null;
        Class<?> declaringType = javaType.getDeclaringClass();
        if (declaringType != null) {
            if (!Modifier.isPublic(javaType.getModifiers()) || !Modifier.isStatic(javaType.getModifiers())) {
                throw new IllegalDocMetaException(
                        "Cannot generate documentation for \"" +
                                javaType.getName() +
                                "\", it is nested class but is not public and static"
                );
            }
            declaringObjectType = create(ctx, declaringType, Collections.emptyList());
        }

        impl = new StaticObjectTypeImpl((StaticObjectTypeImpl) declaringObjectType, javaType, typeArguments);
        ctx.addStaticObjectType(impl);

        if (declaringObjectType != null && typeArguments != null && !typeArguments.isEmpty()) {
            create(ctx, javaType, null);
        }

        NavigableMap<String, Property> props = new TreeMap<>();
        collectProps(ctx, javaType, props);
        impl.props = Collections.unmodifiableNavigableMap(props);
        return impl;
    }

    private static void collectProps(Context ctx, Class<?> javaType, Map<String, Property> props) {
        if (javaType == null || javaType == Object.class) {
            return;
        }
        if (javaType.isAnnotationPresent(ExportFields.class)) {
            for (Field field : javaType.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    Type type = ctx.parseType(field.getAnnotatedType());
                    if (ctx.getJetBrainsMetadata(javaType).isNullable(field)) {
                        type = NullableTypeImpl.of(type);
                    }
                    props.putIfAbsent(field.getName(), new PropertyImpl(field.getName(), type, DocumentImpl.of(field)));
                }
            }
        } else {
            for (Method method : javaType.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameters().length != 0 || method.getReturnType() == void.class) {
                    continue;
                }
                String prefix = null;
                if (method.getReturnType() == boolean.class && method.getName().startsWith("is")) {
                    prefix = "is";
                } else if (method.getName().startsWith("get")) {
                    prefix = "get";
                }
                if (prefix == null) {
                    continue;
                }
                if (method.getTypeParameters().length != 0) {
                    throw new IllegalDocMetaException(
                            "Illegal getter method \"" +
                                    method +
                                    "\", it can not have type parameters"
                    );
                }
                String name = method.getName().substring(prefix.length());
                if (name.isEmpty()) {
                    continue;
                }
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                Type type = ctx.parseType(method.getAnnotatedReturnType());
                if (ctx.getJetBrainsMetadata(javaType).isNullable(method)) {
                    type = NullableTypeImpl.of(type);
                }
                props.put(name, new PropertyImpl(name, type, DocumentImpl.of(method)));
            }
        }
        collectProps(ctx, javaType.getAnnotatedSuperclass(), props);
        for (AnnotatedType itf : javaType.getAnnotatedInterfaces()) {
            collectProps(ctx, itf, props);
        }
    }

    private static void collectProps(Context ctx, AnnotatedType type, Map<String, Property> props) {
        if (type instanceof AnnotatedParameterizedType) {
            Class<?> rawType = (Class<?>) ((ParameterizedType)type.getType()).getRawType();
            collectProps(new Context(ctx, ((AnnotatedParameterizedType)type)), rawType, props);
        } else if (type != null) {
            Class<?> rawType = (Class<?>) type.getType();
            collectProps(ctx, rawType, props);
        }
    }

    private static void collectProps(Context ctx, KClass<?> kotlinType, Map<String, Property> props) {
        for (KProperty1<?, ?> prop : KClasses.getDeclaredMemberProperties(kotlinType)) {
            Type type = ctx.parseKotlinType(prop.getReturnType());
            if (prop.getReturnType().isMarkedNullable()) {
                type = NullableTypeImpl.of(type);
            }
            Method method = ReflectJvmMapping.getJavaGetter(prop);
            props.putIfAbsent(
                    prop.getName(),
                    new PropertyImpl(
                            prop.getName(),
                            type,
                            DocumentImpl.of(
                                    method != null ?
                                            method :
                                            ReflectJvmMapping.getJavaField(prop)
                            )
                    )
            );
        }
        for (KType superType : kotlinType.getSupertypes()) {
            collectProps(ctx, superType, props);
        }
    }

    private static void collectProps(Context ctx, KType type, Map<String, Property> props) {
        if (!type.getArguments().isEmpty()) {
            KClass<?> rawClass = (KClass<?>) type.getClassifier();
            collectProps(new Context(ctx, type), rawClass, props);
        } else {
            KClass<?> rawClass = (KClass<?>) type.getClassifier();
            collectProps(ctx, rawClass, props);
        }
    }
}
