package org.babyfish.jimmer.spring.repository.bytecode;

import org.checkerframework.checker.units.qual.C;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JavaClasses {

    private static final Method PRIVATE_LOOKUP_IN;

    private static final Method LOOKUP_DEFINE_CLASS;

    private static final Method CLASS_LOADER_DEFINE_CLASS;

    private JavaClasses() {}

    public static Class<?> define(byte[] bytecode, Class<?> repositoryInterface) {
        try {
            return LOOKUP_DEFINE_CLASS != null ?
                    defineForJava9(bytecode, repositoryInterface) :
                    defineForJava8(bytecode, repositoryInterface);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(
                    "Cannot create implementation class for \"" +
                            repositoryInterface +
                            "\"",
                    ex
            );
        } catch (InvocationTargetException ex) {
            throw new IllegalArgumentException(
                    "Cannot create implementation class for \"" +
                            repositoryInterface +
                            "\"",
                    ex.getTargetException()
            );
        }
    }

    private static Class<?> defineForJava9(byte[] bytecode, Class<?> repositoryInterface) throws InvocationTargetException, IllegalAccessException {
        Object lookup = PRIVATE_LOOKUP_IN.invoke(null, repositoryInterface, MethodHandles.lookup());
        return (Class<?>) LOOKUP_DEFINE_CLASS.invoke(lookup, (Object) bytecode);
    }

    private static Class<?> defineForJava8(byte[] bytecode, Class<?> repositoryInterface) throws InvocationTargetException, IllegalAccessException {
        return (Class<?>) CLASS_LOADER_DEFINE_CLASS.invoke(
                repositoryInterface.getClassLoader(),
                ClassCodeWriter.implementationClassName(repositoryInterface),
                bytecode,
                0,
                bytecode.length
        );
    }

    static {
        Method privateLookupIn = null;
        try {
            privateLookupIn = MethodHandles.class.getMethod(
                    "privateLookupIn",
                    Class.class,
                    MethodHandles.Lookup.class
            );
        } catch (NoSuchMethodException ex) {
            // Do nothing
        }

        Method lookupDefineClass = null;
        if (privateLookupIn != null) {
            try {
                lookupDefineClass = MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
            } catch (NoSuchMethodException ex) {
                throw new AssertionError("No `Lookup.defineClass`", ex);
            }
        }

        Method classLoaderDefineClass = null;
        if (lookupDefineClass == null) {
            try {
                classLoaderDefineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
                classLoaderDefineClass.setAccessible(true);
            } catch (NoSuchMethodException ex) {
                throw new AssertionError("No `ClassLoader.defineClass`", ex);
            }
        }

        PRIVATE_LOOKUP_IN = privateLookupIn;
        LOOKUP_DEFINE_CLASS = lookupDefineClass;
        CLASS_LOADER_DEFINE_CLASS = classLoaderDefineClass;
    }
}
