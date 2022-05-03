package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meata.ImmutableType;

import java.util.function.BiFunction;

public interface ImmutableSpi {

    boolean __isLoaded(String prop);

    <T> T __get(String prop);

    int __hashCode(boolean shallow);

    boolean __equals(Object obj, boolean shallow);

    ImmutableType __type();

    static boolean equals(Object a, Object b, boolean shallow) {
        return a != null ? ((ImmutableSpi)a).__equals(b, shallow) : b == null;
    }
}
