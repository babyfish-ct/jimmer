package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.Opcodes;
import org.babyfish.jimmer.impl.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class KotlinMethodCodeWriter extends MethodCodeWriter {

    protected KotlinMethodCodeWriter(ClassCodeWriter parent, Method method) {
        super(parent, method);
    }

    @Override
    public void write() {
        if (method.isDefault()) {
            return;
        }
        Method defaultMethod = findDefaultMethod();
        if (defaultMethod != null) {
            writeDefaultInvocation(defaultMethod);
            return;
        }
        throw new IllegalStateException(
                "The current version does not support spring-data-style abstract custom method \"" +
                        method +
                        "\", please wait for the next version. " +
                        "Now, you can write strongly typed DSL using java-default method."
        );
    }

    private void writeDefaultInvocation(Method defaultMethod) {
        mv = parent.getClassVisitor().visitMethod(
                Opcodes.ACC_PUBLIC,
                method.getName(),
                Type.getMethodDescriptor(method),
                null,
                null
        );
        mv.visitCode();
        VarLoader loader = new VarLoader(mv, 0);
        for (Class<?> type : defaultMethod.getParameterTypes()) {
            loader.load(type);
        }
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                parent.getInterfaceInternalName(),
                method.getName(),
                Type.getMethodDescriptor(defaultMethod),
                false
        );
        visitReturn(method.getReturnType());
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private Method findDefaultMethod() {
        Class<?> repositoryInterface = parent.getMetadata().getRepositoryInterface();
        Class<?> defaultImpl = null;
        for (Class<?> nestedClass : repositoryInterface.getClasses()) {
            if (nestedClass.getSimpleName().equals("DefaultImpls")) {
                defaultImpl = nestedClass;
                break;
            }
        }
        if (defaultImpl == null) {
            return null;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?>[] newParameterTypes = new Class[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, newParameterTypes, 1, parameterTypes.length);
        newParameterTypes[0] = repositoryInterface;
        Method defaultImplMethod;
        try {
            defaultImplMethod = defaultImpl.getMethod(method.getName(), newParameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
        if (Modifier.isStatic(defaultImplMethod.getModifiers())) {
            return defaultImplMethod;
        }
        return null;
    }
}
