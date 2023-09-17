package org.babyfish.jimmer.impl.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Helper class for kotlin.
 * <p>
 * In kotlin, `method.invoke(target, *args)` will throw NPE if `args` is null.
 * However, the `InvocationHandler` of JDK proxy may give null `args`.
 * <ul>
 *     <li>In kotlin, `method.invoke(target, args)` is different with java, it wraps `args` into a new array</li>
 *     <li>`method.invoke(target, *(args ?: emptyArray&lt;Any?&gt;()))` has unnecessary cost</li>
 * </ul>
 * The better practice is to never use kotlin to implement `InvocationHandler`.
 * </p>
 */
public class InvocationDelegate implements InvocationHandler {

    private final Object target;

    public InvocationDelegate(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }
}
