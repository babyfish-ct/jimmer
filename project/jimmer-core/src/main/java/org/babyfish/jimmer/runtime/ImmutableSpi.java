package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;

public interface ImmutableSpi {

    boolean __isLoaded(PropId prop);

    boolean __isLoaded(String prop);

    boolean __isVisible(PropId prop);

    boolean __isVisible(String prop);

    Object __get(PropId prop);

    Object __get(String prop);

    int __hashCode(boolean shallow);

    boolean __equals(Object obj, boolean shallow);

    ImmutableType __type();

    static boolean equals(Object a, Object b, boolean shallow) {
        return a != null ? ((ImmutableSpi)a).__equals(b, shallow) : b == null;
    }
}
