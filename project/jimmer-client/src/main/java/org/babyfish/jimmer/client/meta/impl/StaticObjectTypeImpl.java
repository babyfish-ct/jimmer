package org.babyfish.jimmer.client.meta.impl;

import kotlin.jvm.JvmClassMappingKt;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaticObjectTypeImpl implements StaticObjectType {

    private final Class<?> javaType;

    private final KClass<?> kotlinType;

    private final List<Type> typeArguments;

    private Map<String, Property> props;

    private final Document document;

    StaticObjectTypeImpl(Class<?> javaType, List<Type> typeArguments) {
        this.javaType = javaType;
        this.kotlinType = null;
        this.typeArguments = typeArguments != null ?
                Collections.unmodifiableList(typeArguments) :
                Collections.emptyList();
        this.document = DocumentImpl.of(javaType);
    }

    StaticObjectTypeImpl(KClass<?> kotlinType, List<Type> typeArguments) {
        this.javaType = JvmClassMappingKt.getJavaClass(kotlinType);
        this.kotlinType = kotlinType;
        this.typeArguments = typeArguments != null ?
                Collections.unmodifiableList(typeArguments) :
                Collections.emptyList();
        this.document = DocumentImpl.of(javaType);
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
            visitor.visitStaticObjectType(this);
            for (Property prop : props.values()) {
                prop.getType().accept(visitor);
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

        impl = new StaticObjectTypeImpl(javaType, typeArguments);
        ctx.addStaticObjectType(impl);

        Map<String, Property> props = new LinkedHashMap<>();
        collectProps(ctx, javaType, props);
        impl.props = Collections.unmodifiableMap(props);
        return impl;
    }

    static StaticObjectType create(Context ctx, KClass<?> kotlinType, List<Type> typeArguments) {

        StaticObjectTypeImpl impl = (StaticObjectTypeImpl) ctx.getStaticObjectType(
                JvmClassMappingKt.getJavaClass(kotlinType),
                typeArguments
        );
        if (impl != null) {
            return impl;
        }

        impl = new StaticObjectTypeImpl(kotlinType, typeArguments);
        ctx.addStaticObjectType(impl);

        Map<String, Property> props = new LinkedHashMap<>();
        collectProps(ctx, kotlinType, props);
        impl.props = Collections.unmodifiableMap(props);
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
                if (method.getParameters().length != 0 || method.getReturnType() == void.class) {
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
