package org.babyfish.jimmer.sql.common;

import org.junit.jupiter.api.Assertions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public final class ProxyRecorder<T> implements InvocationHandler {

    private final Class<T> type;

    private final Map<String, Object> returns = new HashMap<>();

    private final Map<String, List<Object[]>> calls = new HashMap<>();

    private T proxy;

    private ProxyRecorder(Class<T> type) {
        this.type = type;
    }

    public static <T> ProxyRecorder<T> of(Class<T> type) {
        return new ProxyRecorder<>(type);
    }

    @SuppressWarnings("unchecked")
    public T proxy() {
        if (proxy == null) {
            proxy = (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[] {type},
                    this
            );
        }
        return proxy;
    }

    public ProxyRecorder<T> returns(String methodName, Object value) {
        returns.put(methodName, value);
        return this;
    }

    public void assertCalledOnce(String methodName) {
        Assertions.assertEquals(
                1,
                calls.getOrDefault(methodName, Collections.emptyList()).size(),
                "Expected method \"" + methodName + "\" to be called once"
        );
    }

    public void assertCalledOnceWith(String methodName, Object... args) {
        assertCalledOnce(methodName);
        Assertions.assertArrayEquals(args, calls.get(methodName).get(0));
    }

    public void assertNeverCalled(String methodName) {
        Assertions.assertEquals(
                0,
                calls.getOrDefault(methodName, Collections.emptyList()).size(),
                "Expected method \"" + methodName + "\" not to be called"
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            switch (methodName) {
                case "toString":
                    return "ProxyRecorder(" + type.getName() + ")";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return args != null && args.length == 1 && proxy == args[0];
                default:
                    throw new AssertionError("Unexpected Object method: " + methodName);
            }
        }
        calls.computeIfAbsent(methodName, it -> new ArrayList<>()).add(copy(args));
        if (returns.containsKey(methodName)) {
            return returns.get(methodName);
        }
        return defaultValue(method.getReturnType());
    }

    private static Object[] copy(Object[] args) {
        return args != null ? Arrays.copyOf(args, args.length) : new Object[0];
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }
}
