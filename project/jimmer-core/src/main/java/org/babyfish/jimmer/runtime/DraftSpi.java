package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;

public interface DraftSpi extends Draft, ImmutableSpi {

    void __unload(int prop);

    void __unload(String prop);

    void __set(int prop, Object value);

    void __set(String prop, Object value);

    void __show(int prop, boolean show);

    void __show(String prop, boolean show);

    DraftContext __draftContext();

    Object __resolve();
}
