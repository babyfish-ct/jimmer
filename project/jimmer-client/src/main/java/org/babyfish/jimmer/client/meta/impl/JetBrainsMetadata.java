package org.babyfish.jimmer.client.meta.impl;

import kotlin.Metadata;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.*;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.impl.asm.*;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

class JetBrainsMetadata {

    private static final String JETBRAINS_NULLABLE_DESC = Type.getDescriptor(Nullable.class);

    private static final String SPRING_NULLABLE = "org.springframework.lang.Nullable";

    private static final Set<Class<?>> BOX_TYPES = new HashSet<>(
            Arrays.asList(
                    Boolean.class,
                    Character.class,
                    Byte.class,
                    Short.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class
            )
    );

    private final Class<?> javaClass;

    private final KClass<?> kotlinClass;

    private final Map<AccessibleObject, KCallable<?>> kCallableMap;

    private Nullity nullity;

    JetBrainsMetadata(Class<?> javaClass) {
        this.javaClass = javaClass;
        if (javaClass.isAnnotationPresent(Metadata.class)) {
            this.kotlinClass = JvmClassMappingKt.getKotlinClass(javaClass);
            Map<AccessibleObject, KCallable<?>> callableMap = new HashMap<>();
            for (KProperty1<?, ?> prop : KClasses.getDeclaredMemberProperties(kotlinClass)) {
                Field field = ReflectJvmMapping.getJavaField(prop);
                if (field != null) {
                    callableMap.put(field, prop);
                }
                Method getter = ReflectJvmMapping.getJavaGetter(prop);
                if (getter != null) {
                    callableMap.put(getter, prop);
                }
                if (prop instanceof KMutableProperty<?>) {
                    Method setter = ReflectJvmMapping.getJavaSetter((KMutableProperty<?>) prop);
                    if (getter != null) {
                        callableMap.put(setter, prop);
                    }
                }
            }
            for (KFunction<?> func : KClasses.getDeclaredFunctions(kotlinClass)) {
                Method method = ReflectJvmMapping.getJavaMethod(func);
                callableMap.put(method, func);
            }
            this.kCallableMap = callableMap;
        } else {
            this.kotlinClass = null;
            this.kCallableMap = null;
        }
    }

    public boolean isKotlinClass() {
        return kotlinClass != null;
    }

    public KFunction<?> toKFunction(Method method) {
        if (kCallableMap == null) {
            throw new IllegalStateException(
                    "The current class \"" +
                            javaClass.getName() +
                            "\" is not kotlin class"
            );
        }
        return (KFunction<?>) kCallableMap.get(method);
    }

    private Nullity getNullity() {
        Nullity nty = nullity;
        if (nty == null) {
            if (kotlinClass != null) {
                throw new IllegalStateException("It is unnecessary to create nullity for kotlin class");
            }
            nullity = nty = new Nullity(javaClass);
        }
        return nty;
    }

    public boolean isNullable(Field field) {
        return isNullable(field, field.getType());
    }

    public boolean isNullable(Method method) {
        return isNullable(method, method.getReturnType());
    }

    private boolean isNullable(AccessibleObject member, Class<?> type) {
        if (kCallableMap != null) {
            KCallable<?> callable = kCallableMap.get(member);
            return callable != null && callable.getReturnType().isMarkedNullable();
        }
        if (type.isPrimitive()) {
            return false;
        }
        if (BOX_TYPES.contains(type)) {
            return true;
        }
        if (member.isAnnotationPresent(Null.class)) {
            return true;
        }
        for (Annotation annotation : member.getAnnotations()) {
            if (annotation.annotationType().getName().equals(SPRING_NULLABLE)) {
                return true;
            }
        }
        return getNullity().nullableMembers.contains(member);
    }

    public boolean isNullable(Method method, int parameterIndex) {
        if (kCallableMap != null) {
            KCallable<?> callable = kCallableMap.get(method);
            return callable != null && callable.getParameters().get(parameterIndex).getType().isMarkedNullable();
        }
        Set<Integer> indices = getNullity().nullableParameterIndices.get(method);
        return indices != null && indices.contains(parameterIndex);
    }

    private static class Nullity {

        private final Map<Member, AccessibleObject> accessibleObjectMap = new HashMap<>();

        final Set<AccessibleObject> nullableMembers = new HashSet<>();

        final Map<AccessibleObject, Set<Integer>> nullableParameterIndices = new HashMap<>();

        Nullity(Class<?> clazz) {
            Map<Member, AccessibleObject> accessibleObjectMap = new HashMap<>();
            for (Field field : clazz.getDeclaredFields()) {
                accessibleObjectMap.put(new Member(field.getName(), Type.getDescriptor(field.getType())), field);
            }
            for (Method method : clazz.getDeclaredMethods()) {
                accessibleObjectMap.put(new Member(method.getName(), Type.getMethodDescriptor(method)), method);
            }
            try {
                ClassReader reader = new ClassReader(clazz.getName());
                reader.accept(
                        new ClassVisitor(Opcodes.ASM9, null) {

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                return (access & Opcodes.ACC_STATIC) == 0 ?
                                        new FieldVisitorImpl(accessibleObjectMap.get(new Member(name, descriptor))) :
                                        null;
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                return (access & Opcodes.ACC_STATIC) == 0 ?
                                        new MethodVisitorImpl(accessibleObjectMap.get(new Member(name, descriptor))) :
                                        null;
                            }
                        },
                        ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG
                );
            } catch (IOException ex) {
                throw new IllegalDocMetaException(
                        "Failed to parse the jetbrains nullity for class \"" +
                                clazz.getName() +
                                "\""
                );
            }
        }

        private class FieldVisitorImpl extends FieldVisitor {

            private final AccessibleObject field;

            protected FieldVisitorImpl(AccessibleObject field) {
                super(Opcodes.ASM9);
                this.field = field;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals(JETBRAINS_NULLABLE_DESC)) {
                    nullableMembers.add(field);
                }
                return null;
            }
        }

        private class MethodVisitorImpl extends MethodVisitor {

            private final AccessibleObject method;

            protected MethodVisitorImpl(AccessibleObject method) {
                super(Opcodes.ASM9);
                this.method = method;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals(JETBRAINS_NULLABLE_DESC)) {
                    nullableMembers.add(method);
                }
                return null;
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                if (descriptor.equals(JETBRAINS_NULLABLE_DESC)) {
                    nullableParameterIndices
                            .computeIfAbsent(method, it -> new HashSet<>())
                            .add(parameter);
                }
                return null;
            }
        }
    }

    private static class Member {

        final String name;

        final String descriptor;

        public Member(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Member member = (Member) o;
            return name.equals(member.name) && descriptor.equals(member.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, descriptor);
        }

        @Override
        public String toString() {
            return name + ':' + descriptor;
        }
    }
}
