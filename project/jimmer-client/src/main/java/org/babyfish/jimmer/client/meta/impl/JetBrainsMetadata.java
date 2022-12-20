package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.impl.asm.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

class JetBrainsMetadata {

    private static final String JETBRAINS_NULLABLE_DESC = Type.getDescriptor(Nullable.class);

    private final Set<String> nullableFields = new HashSet<>();

    private final Set<String> nullableMethods = new HashSet<>();

    private final Map<String, Set<Integer>> nullableParameterIndices = new HashMap<>();

    JetBrainsMetadata(Class<?> clazz) {
        try {
            ClassReader reader = new ClassReader(clazz.getName());
            reader.accept(
                    new ClassVisitor(Opcodes.ASM9, null) {

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            return (access & Opcodes.ACC_STATIC) == 0 ? new FieldVisitorImpl(name) : null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return (access & Opcodes.ACC_STATIC) == 0 ? new MethodVisitorImpl(name) : null;
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

    public boolean isNull(String name) {
        return nullableFields.contains(name) || nullableMethods.contains(name);
    }

    public boolean isNull(String methodName, int parameterIndex) {
        Set<Integer> indices = nullableParameterIndices.get(methodName);
        return indices != null && indices.contains(parameterIndex);
    }

    private class FieldVisitorImpl extends FieldVisitor {

        private final String name;

        protected FieldVisitorImpl(String name) {
            super(Opcodes.ASM9);
            this.name = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals(JETBRAINS_NULLABLE_DESC)) {
                nullableFields.add(name);
            }
            return null;
        }
    }

    private class MethodVisitorImpl extends MethodVisitor {

        private final String name;

        protected MethodVisitorImpl(String name) {
            super(Opcodes.ASM9);
            this.name = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals(JETBRAINS_NULLABLE_DESC)) {
                nullableMethods.add(name);
            }
            return null;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            if (descriptor.equals(JETBRAINS_NULLABLE_DESC)) {
                nullableParameterIndices
                        .computeIfAbsent(name, it -> new HashSet<>())
                        .add(parameter);
            }
            return null;
        }
    }
}
