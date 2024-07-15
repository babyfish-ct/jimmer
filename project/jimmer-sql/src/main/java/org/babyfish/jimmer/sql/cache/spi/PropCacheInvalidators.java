package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.sql.cache.PropCacheInvalidator;
import org.babyfish.jimmer.sql.filter.impl.FilterWrapper;
import org.babyfish.jimmer.sql.di.AopProxyProvider;

import java.lang.reflect.Method;
import java.util.Collection;

public class PropCacheInvalidators {

    public PropCacheInvalidators() {}

    public static boolean isGetAffectedSourceIdsOverridden(
            Object o,
            Class<?> eventType,
            AopProxyProvider aopProxyProvider
    ) {
        if (o instanceof Collection<?>) {
            return isGetAffectedSourceIdsOverridden((Collection<?>) o, eventType, aopProxyProvider);
        }
        return isGetAffectedSourceIdsOverridden0(o, eventType, aopProxyProvider);
    }

    public static boolean isGetAffectedSourceIdsOverridden(
            Collection<?> objects,
            Class<?> entityType,
            AopProxyProvider aopProxyProvider
    ) {
        for (Object o : objects) {
            if (isGetAffectedSourceIdsOverridden0(o, entityType, aopProxyProvider)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGetAffectedSourceIdsOverridden0(
            Object o,
            Class<?> eventType,
            AopProxyProvider aopProxyProvider
    ) {
        o = FilterWrapper.unwrap(o);
        if (!(o instanceof PropCacheInvalidator)) {
            return false;
        }
        Class<?> type = null;
        if (aopProxyProvider != null) {
            type = aopProxyProvider.getTargetClass(o);
        }
        if (type == null) {
            type = o.getClass();
        }
        Method method;
        try {
            method = type.getMethod("getAffectedSourceIds", eventType);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Internal bug: No `getAffectedSourceIds(" + eventType.getName() + ")`");
        }
        return method.getDeclaringClass() != PropCacheInvalidator.class;
    }
}
