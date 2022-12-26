package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.ClassWriter;
import org.babyfish.jimmer.impl.asm.MethodVisitor;
import org.babyfish.jimmer.impl.asm.Opcodes;
import org.babyfish.jimmer.impl.asm.Type;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.springframework.data.repository.core.RepositoryInformation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ClassCodeWriter implements Constants {

    public static final String ASM_IMPL_SUFFIX = "{AsmImpl}";

    final RepositoryInformation metadata;

    private final Class<?> superType;

    private final String interfaceInternalName;

    private final String implInternalName;

    private final String superInternalName;

    private final String entityInternalName;

    private final String sqlClientDescriptor;

    protected final List<MethodCodeWriter> methodCodeWriters;

    protected final List<MethodCodeWriter> autoGenMethodCodeWriters;

    private ClassWriter cw;

    Context ctx = new Context();

    protected ClassCodeWriter(RepositoryInformation metadata, Class<?> sqlClientType, Class<?> superType) {
        this.metadata = metadata;
        this.superType = superType;
        this.interfaceInternalName = Type.getInternalName(metadata.getRepositoryInterface());
        this.implInternalName = interfaceInternalName + ASM_IMPL_SUFFIX;
        this.superInternalName = Type.getInternalName(superType);
        this.entityInternalName = Type.getInternalName(metadata.getDomainType());
        this.sqlClientDescriptor = Type.getDescriptor(sqlClientType);
        Class<?> repositoryInterface = metadata.getRepositoryInterface();
        List<MethodCodeWriter> list = new ArrayList<>();
        Map<String, Integer> methodNameCountMap = new HashMap<>();
        for (Method method : repositoryInterface.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) &&
                    !method.getDeclaringClass().isAssignableFrom(superType)) {
                Integer count = methodNameCountMap.get(method.getName());
                if (count == null) {
                    list.add(createMethodCodeWriter(method, method.getName()));
                    methodNameCountMap.put(method.getName(), 1);
                } else {
                    ++count;
                    list.add(createMethodCodeWriter(method, method.getName() + ":" + count));
                    methodNameCountMap.put(method.getName(), count);
                }
            }
        }
        this.methodCodeWriters = list;
        this.autoGenMethodCodeWriters = list
                .stream()
                .filter(it -> !it.method.isDefault() && it.getDefaultImplMethod() == null)
                .collect(Collectors.toList());
    }

    public RepositoryInformation getMetadata() {
        return metadata;
    }

    public String getInterfaceInternalName() {
        return interfaceInternalName;
    }

    public String getImplInternalName() {
        return implInternalName;
    }

    public String getEntityInternalName() {
        return entityInternalName;
    }

    public ClassWriter getClassVisitor() {
        return cw;
    }

    public byte[] write() {
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                implInternalName,
                null,
                superInternalName,
                new String[] { interfaceInternalName }
        );
        writeInit();
        if (!autoGenMethodCodeWriters.isEmpty()) {
            writeStaticFields();
            writeClinit();
        }
        for (MethodCodeWriter writer : methodCodeWriters) {
            writer.write();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void writeInit() {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                '(' + sqlClientDescriptor + ")V",
                null,
                null
        );
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn(Type.getType(metadata.getDomainType()));
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                superInternalName,
                "<init>",
                "(" + sqlClientDescriptor + "Ljava/lang/Class;)V",
                false
        );
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void writeStaticFields() {
        for (MethodCodeWriter writer : autoGenMethodCodeWriters) {
            cw.visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                    writer.queryMethodFieldName(),
                    QUERY_METHOD_DESCRIPTOR,
                    null,
                    null
            ).visitEnd();
        }
    }

    private void writeClinit() {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );
        mv.visitCode();

        mv.visitTypeInsn(Opcodes.NEW, CONTEXT_INTERNAL_NAME);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                CONTEXT_INTERNAL_NAME,
                "<init>",
                "()V",
                false
        );
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        mv.visitLdcInsn(Type.getType(metadata.getDomainType()));
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                IMMUTABLE_TYPE_INTERNAL_NAME,
                "get",
                "(Ljava/lang/Class;)" + IMMUTABLE_TYPE_DESCRIPTOR,
                true
        );
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        for (MethodCodeWriter writer : autoGenMethodCodeWriters) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(Type.getType(metadata.getRepositoryInterface()));
            mv.visitLdcInsn(writer.method.getName());
            mv.visitLdcInsn(writer.method.getParameterTypes().length);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
            int index = 0;
            for (Class<?> clazz : writer.method.getParameterTypes()) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(index++);
                mv.visitLdcInsn(Type.getType(clazz));
                mv.visitInsn(Opcodes.AASTORE);
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getMethod",
                    "(Ljava/lang/String;[Ljava/lang/Class;)" + METHOD_DESCRIPTOR,
                    false
            );
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    QUERY_METHOD_INTERNAL_NAME,
                    "of",
                    "(" + CONTEXT_DESCRIPTOR + IMMUTABLE_TYPE_DESCRIPTOR + METHOD_DESCRIPTOR + ")" +
                            QUERY_METHOD_DESCRIPTOR,
                    false
            );
            mv.visitFieldInsn(
                    Opcodes.PUTSTATIC,
                    implInternalName,
                    writer.queryMethodFieldName(),
                    QUERY_METHOD_DESCRIPTOR
            );
        }
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    protected abstract MethodCodeWriter createMethodCodeWriter(Method method, String id);

    public static String implementationClassName(Class<?> itf) {
        return itf.getName() + ASM_IMPL_SUFFIX;
    }
}
