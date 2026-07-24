package org.babyfish.jimmer.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public final class ProxyRecorder<T> implements InvocationHandler {

    private final Class<T> type;

    private Object target;

    private final Map<String, Object> returns = new HashMap<>();

    private final Map<String, MethodHandler> handlers = new HashMap<>();

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

    public ProxyRecorder<T> delegatesTo(Object target) {
        this.target = target;
        return this;
    }

    public ProxyRecorder<T> handles(String methodName, MethodHandler handler) {
        handlers.put(methodName, handler);
        return this;
    }

    public List<Object[]> calls(String methodName) {
        return calls.getOrDefault(methodName, Collections.emptyList());
    }

    public void assertCalledOnce(String methodName) {
        int size = calls.getOrDefault(methodName, Collections.emptyList()).size();
        if (size != 1) {
            throw new AssertionError(
                    "Expected method \"" + methodName + "\" to be called once, but it was called " + size + " time(s)"
            );
        }
    }

    public void assertCalledOnceWith(String methodName, Object... args) {
        assertCalledOnce(methodName);
        if (!Arrays.equals(args, calls.get(methodName).get(0))) {
            throw new AssertionError(
                    "Expected method \"" + methodName + "\" to be called with " +
                            Arrays.toString(args) +
                            ", but got " +
                            Arrays.toString(calls.get(methodName).get(0))
            );
        }
    }

    public void assertNeverCalled(String methodName) {
        int size = calls.getOrDefault(methodName, Collections.emptyList()).size();
        if (size != 0) {
            throw new AssertionError(
                    "Expected method \"" + methodName + "\" not to be called, but it was called " + size + " time(s)"
            );
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        MethodHandler handler = handlers.get(methodName);
        if (handler != null) {
            return handler.invoke(method, args);
        }
        if (target != null) {
            return invokeTarget(target, method, args);
        }
        return defaultValue(method.getReturnType());
    }

    public static Object invokeTarget(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Cannot invoke delegated method: " + method, ex);
        }
    }

    public interface MethodHandler {

        Object invoke(Method method, Object[] args) throws Throwable;
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
