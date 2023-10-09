package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.PropCacheInvalidator;
import org.babyfish.jimmer.sql.filter.impl.FilterWrapper;

import java.util.Collection;

public class PropCacheInvalidators {

    public PropCacheInvalidators() {}

    public static boolean isGetAffectedSourceIdsOverridden(Object o, Class<?> eventType) {
        if (o instanceof Collection<?>) {
            return isGetAffectedSourceIdsOverridden((Collection<?>) o, eventType);
        }
        return isGetAffectedSourceIdsOverridden0(o, eventType);
    }

    public static boolean isGetAffectedSourceIdsOverridden(Collection<?> objects, Class<?> entityType) {
        for (Object o : objects) {
            if (isGetAffectedSourceIdsOverridden0(o, entityType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGetAffectedSourceIdsOverridden0(Object o, Class<?> eventType) {
        o = FilterWrapper.unwrap(o);
        if (!(o instanceof PropCacheInvalidator)) {
            return false;
        }
        try {
            o.getClass().getDeclaredMethod("getAffectedSourceIds", eventType);
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }
}
