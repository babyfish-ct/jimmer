package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.ClassWriter;
import org.babyfish.jimmer.impl.asm.MethodVisitor;
import org.babyfish.jimmer.impl.asm.Opcodes;
import org.babyfish.jimmer.impl.asm.Type;
import org.springframework.data.repository.core.RepositoryInformation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class ClassCodeWriter {

    public static final String ASM_IMPL_SUFFIX = "{AsmImpl}";

    private final RepositoryInformation metadata;

    private final Class<?> superType;

    private final String interfaceInternalName;

    private final String implInternalName;

    private final String superInternalName;

    private final String entityInternalName;

    private final String sqlClientInternalName;

    private final String sqlClientDescriptor;

    private ClassWriter cw;

    protected ClassCodeWriter(RepositoryInformation metadata, Class<?> sqlClientType, Class<?> superType) {
        this.metadata = metadata;
        this.superType = superType;
        this.interfaceInternalName = Type.getInternalName(metadata.getRepositoryInterface());
        this.implInternalName = interfaceInternalName + ASM_IMPL_SUFFIX;
        this.superInternalName = Type.getInternalName(superType);
        this.entityInternalName = Type.getInternalName(metadata.getDomainType());
        this.sqlClientInternalName = Type.getInternalName(sqlClientType);
        this.sqlClientDescriptor = Type.getDescriptor(sqlClientType);
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

    public String getSqlClientInternalName() {
        return sqlClientInternalName;
    }

    public String getSqlClientDescriptor() {
        return sqlClientDescriptor;
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
        Class<?> repositoryInterface = metadata.getRepositoryInterface();
        for (Method method : repositoryInterface.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) &&
                    !method.getDeclaringClass().isAssignableFrom(superType)) {
                createMethodCodeWriter(method).write();
            }
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

    protected MethodCodeWriter createMethodCodeWriter(Method method) {
        return new MethodCodeWriter(this, method);
    }

    public static String implementationClassName(Class<?> itf) {
        return itf.getName() + ASM_IMPL_SUFFIX;
    }
}
