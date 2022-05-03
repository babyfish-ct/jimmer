package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;

public interface DraftSpi extends Draft {

    void __unload(String prop);

    void __set(String prop, Object value);

    DraftContext __draftContext();

    Object __resolve();
}
