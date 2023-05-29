package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;

public class DraftObjects {

    private DraftObjects() {}

    public static void set(Object draft, PropId prop, Object value) {
        ((DraftSpi)draft).__set(prop, value);
    }

    public static void set(Object draft, String prop, Object value) {
        ((DraftSpi)draft).__set(prop, value);
    }

    public static void set(Object draft, ImmutableProp prop, Object value) {
        ((DraftSpi)draft).__set(prop.getId(), value);
    }

    public static void set(Object draft, TypedProp<?, ?> prop, Object value) {
        ((DraftSpi)draft).__set(prop.unwrap().getId(), value);
    }

    public static void unload(Object draft, PropId prop) {
        ((DraftSpi)draft).__unload(prop);
    }

    public static void unload(Object draft, String prop) {
        ((DraftSpi)draft).__unload(prop);
    }

    public static void unload(Object draft, ImmutableProp prop) {
        ((DraftSpi)draft).__unload(prop.getId());
    }

    public static void unload(Object draft, TypedProp<?, ?> prop) {
        ((DraftSpi)draft).__unload(prop.unwrap().getId());
    }
}
