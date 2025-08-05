package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.impl.org.objectweb.asm.ClassReader;
import org.babyfish.jimmer.impl.org.objectweb.asm.ClassVisitor;
import org.babyfish.jimmer.impl.org.objectweb.asm.MethodVisitor;
import org.babyfish.jimmer.impl.org.objectweb.asm.Opcodes;
import org.babyfish.jimmer.impl.org.objectweb.asm.tree.InsnList;
import org.babyfish.jimmer.impl.org.objectweb.asm.tree.MethodNode;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;

import java.io.IOException;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractWeakJoinLambdaFactory {

    private static final WeakJoinLambda NIL =
            new WeakJoinLambda(new InsnList(), void.class, void.class);

    private final ReadWriteLock cacheRwl = new ReentrantReadWriteLock();

    private final Map<Class<?>, WeakJoinLambda> cacheMap = new WeakHashMap<>();

    protected final WeakJoinLambda getLambda(Object join) {
        WeakJoinLambda weakJoinLambda;
        Lock lock;
        (lock = cacheRwl.readLock()).lock();
        try {
            weakJoinLambda = cacheMap.get(join.getClass());
        } finally {
            lock.unlock();
        }
        if (weakJoinLambda == null) {
            (lock = cacheRwl.writeLock()).lock();
            try {
                weakJoinLambda = cacheMap.get(join.getClass());
                if (weakJoinLambda == null) {
                    weakJoinLambda = create(join);
                    if (weakJoinLambda == null) {
                        weakJoinLambda = NIL;
                    }
                    cacheMap.put(join.getClass(), weakJoinLambda);
                }
            } finally {
                lock.unlock();
            }
        }
        return weakJoinLambda == NIL ? null : weakJoinLambda;
    }

    private WeakJoinLambda create(Object join) {
        SerializedLambda serializedLambda = getSerializedLambda(join);
        if (serializedLambda == null) {
            return null;
        }
        Class<?>[] types = getTypes(serializedLambda);
        ClassReader classReader;
        try {
            classReader = new ClassReader(serializedLambda.getImplClass());
        } catch (IOException ex) {
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
        return new WeakJoinLambda(cv.methodNode.instructions, types[0], types[1]);
    }

    protected abstract Class<?>[] getTypes(SerializedLambda serializedLambda);

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
