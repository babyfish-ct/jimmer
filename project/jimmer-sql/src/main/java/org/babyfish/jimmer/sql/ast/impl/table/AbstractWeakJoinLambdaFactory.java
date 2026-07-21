package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.impl.org.objectweb.asm.ClassReader;
import org.babyfish.jimmer.impl.org.objectweb.asm.ClassVisitor;
import org.babyfish.jimmer.impl.org.objectweb.asm.MethodVisitor;
import org.babyfish.jimmer.impl.org.objectweb.asm.Opcodes;
import org.babyfish.jimmer.impl.org.objectweb.asm.tree.InsnList;
import org.babyfish.jimmer.impl.org.objectweb.asm.tree.MethodNode;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractWeakJoinLambdaFactory {

    private static final Object NIL = new Object();

    private static final ClassCache<LambdaInfoSlot> CACHE =
            new ClassCache<>(it -> new LambdaInfoSlot());

    protected final WeakJoinLambda getLambda(Object join) {
        LambdaInfo lambdaInfo = CACHE.get(join.getClass()).get(join);
        if (lambdaInfo == null) {
            return null;
        }
        Class<?>[] types = getTypes(lambdaInfo.implClass, lambdaInfo.implMethodSignature);
        return lambdaInfo.getLambda(types[0], types[1]);
    }

    protected final WeakJoinLambda getClassInvariantLambda(Object join) {
        LambdaInfo lambdaInfo = CACHE.get(join.getClass()).get(join);
        if (lambdaInfo == null) {
            return null;
        }
        return lambdaInfo.getClassInvariantLambda(this);
    }

    private static LambdaInfo create(Object join) {
        SerializedLambda serializedLambda = getSerializedLambda(join);
        if (serializedLambda == null) {
            return null;
        }
        ClassReader classReader = null;
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                serializedLambda.getImplClass() + ".class"
        );
        if (inputStream != null) {
            try {
                try {
                    classReader = new ClassReader(inputStream);
                } finally {
                    inputStream.close();
                }
            } catch (IOException ex) {
                // Nothing
            }
        }
        if (classReader == null) {
            throw new IllegalStateException(
                    "Cannot read the byte code of \"" +
                            serializedLambda.getImplClass() +
                            "\", is your application running as native code? " +
                            "Two choices: " +
                            "\n1. Use class implementation of " + WeakJoin.class.getName() +
                            "\n2. Run application as JVM mode"
            );
        }
        ClassVisitorImpl cv = new ClassVisitorImpl(serializedLambda.getImplMethodName());
        classReader.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        InsnListUtils.eraseLambdaMagicNumber(cv.methodNode.instructions);
        return new LambdaInfo(
                cv.methodNode.instructions,
                serializedLambda.getImplClass(),
                serializedLambda.getImplMethodSignature()
        );
    }

    protected abstract Class<?>[] getTypes(String implClass, String implMethodSignature);

    private static SerializedLambda getSerializedLambda(Object join) {

        Method writeReplace;
        try {
            writeReplace = join.getClass().getDeclaredMethod("writeReplace");
        } catch (NoSuchMethodException ex) {
            return null;
        }
        writeReplace.setAccessible(true);
        Object serializedLambda;
        try {
            serializedLambda = writeReplace.invoke(join);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("Cannot get writeReplace of lambda " + join, ex);
        }
        if (!(serializedLambda instanceof SerializedLambda)) {
            throw new IllegalStateException("Not a SerializedLambda: " + serializedLambda.getClass());
        }
        return (SerializedLambda) serializedLambda;
    }

    private static class LambdaInfoSlot {

        private volatile Object value;

        LambdaInfo get(Object join) {
            Object value = this.value;
            if (value == null) {
                LambdaInfo lambdaInfo = create(join);
                value = lambdaInfo != null ? lambdaInfo : NIL;
                this.value = value;
            }
            return value != NIL ? (LambdaInfo) value : null;
        }
    }

    private static class LambdaInfo {

        private final InsnList instructions;

        private final String implClass;

        private final String implMethodSignature;

        private volatile WeakJoinLambda primaryLambda;

        private volatile WeakJoinLambda classInvariantLambda;

        private LambdaInfo(
                InsnList instructions,
                String implClass,
                String implMethodSignature
        ) {
            this.instructions = instructions;
            this.implClass = implClass;
            this.implMethodSignature = implMethodSignature;
        }

        WeakJoinLambda getLambda(Class<?> sourceType, Class<?> targetType) {
            WeakJoinLambda lambda = primaryLambda;
            if (lambda != null &&
                    lambda.getSourceType() == sourceType &&
                    lambda.getTargetType() == targetType) {
                return lambda;
            }
            lambda = new WeakJoinLambda(instructions, sourceType, targetType);
            if (primaryLambda == null) {
                primaryLambda = lambda;
            }
            return lambda;
        }

        WeakJoinLambda getClassInvariantLambda(AbstractWeakJoinLambdaFactory factory) {
            WeakJoinLambda lambda = classInvariantLambda;
            if (lambda == null) {
                Class<?>[] types = factory.getTypes(implClass, implMethodSignature);
                lambda = getLambda(types[0], types[1]);
                classInvariantLambda = lambda;
            }
            return lambda;
        }
    }

    private static class ClassVisitorImpl extends ClassVisitor {

        private final String methodName;

        MethodNode methodNode;

        protected ClassVisitorImpl(String methodName) {
            super(Opcodes.ASM5);
            this.methodName = methodName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals(methodName)) {
                return this.methodNode = new MethodNode();
            }
            return null;
        }
    }
}
