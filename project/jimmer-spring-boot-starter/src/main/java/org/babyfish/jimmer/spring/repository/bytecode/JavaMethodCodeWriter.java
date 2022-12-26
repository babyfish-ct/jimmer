package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.Opcodes;

import java.lang.reflect.Method;

public class JavaMethodCodeWriter extends MethodCodeWriter {

    protected JavaMethodCodeWriter(ClassCodeWriter parent, Method method, String id) {
        super(parent, method, id);
    }

    @Override
    protected void visitLoadJSqlClient() {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                parent.getImplInternalName(),
                "sqlClient",
                J_SQL_CLIENT_DESCRIPTOR
        );
    }
}
