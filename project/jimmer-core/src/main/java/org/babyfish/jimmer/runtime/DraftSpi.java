package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;

public interface DraftSpi extends Draft, ImmutableSpi {

    void __unload(int prop);

    void __unload(String prop);

    void __set(int prop, Object value);

    void __set(String prop, Object value);

    void __use(int prop);

    void __use(String prop);

    DraftContext __draftContext();

    Object __resolve();
}
