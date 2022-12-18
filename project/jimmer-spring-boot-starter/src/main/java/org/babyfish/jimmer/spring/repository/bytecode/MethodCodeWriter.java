package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.MethodVisitor;
import org.babyfish.jimmer.impl.asm.Opcodes;

import java.lang.reflect.Method;

public class MethodCodeWriter {

    protected final ClassCodeWriter parent;

    protected final Method method;

    private MethodVisitor mv;

    protected MethodCodeWriter(ClassCodeWriter parent, Method method) {
        this.parent = parent;
        this.method = method;
    }

    public void write() {
        if (!method.isDefault()) {
            throw new IllegalStateException(
                    "The current version does not support spring-data-style abstract custom method \"" +
                            method +
                            "\", please wait for the next version. " +
                            "Now, you can write strongly typed DSL using java-default method."
            );
        }
    }

    protected void visitLoadSqlClient() {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                parent.getImplInternalName(),
                "sqlClient",
                parent.getSqlClientDescriptor()
        );
    }

    public MethodVisitor getMethodVisitor() {
        return mv;
    }

    protected void visitReturn(Class<?> type) {
        if (type == void.class) {
            mv.visitInsn(Opcodes.RETURN);
        } else if (type == boolean.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == char.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == byte.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == short.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == int.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == long.class) {
            mv.visitInsn(Opcodes.LRETURN);
        } else if (type == float.class) {
            mv.visitInsn(Opcodes.FRETURN);
        } else if (type == double.class) {
            mv.visitInsn(Opcodes.DRETURN);
        } else {
            mv.visitInsn(Opcodes.ARETURN);
        }
    }

    protected static class VarLoader {

        private final MethodVisitor mv;

        private int slot;

        public VarLoader(MethodVisitor mv, int slot) {
            this.mv = mv;
            this.slot = slot;
        }

        public void load(Class<?> type) {
            if (type == boolean.class) {
                mv.visitVarInsn(Opcodes.ILOAD, slot++);
            } else if (type == char.class) {
                mv.visitVarInsn(Opcodes.ILOAD, slot++);
            } else if (type == byte.class) {
                mv.visitVarInsn(Opcodes.ILOAD, slot++);
            } else if (type == short.class) {
                mv.visitVarInsn(Opcodes.ILOAD, slot++);
            } else if (type == int.class) {
                mv.visitVarInsn(Opcodes.ILOAD, slot++);
            } else if (type == long.class) {
                mv.visitVarInsn(Opcodes.LLOAD, slot);
                slot += 2;
            } else if (type == float.class) {
                mv.visitVarInsn(Opcodes.FLOAD, slot++);
            } else if (type == double.class) {
                mv.visitVarInsn(Opcodes.DLOAD, slot);
                slot += 2;
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, slot++);
            }
        }
    }
}
