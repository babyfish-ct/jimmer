package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.PropId;

public interface DraftSpi extends Draft, ImmutableSpi {

    void __unload(PropId prop);

    void __unload(String prop);

    void __set(PropId prop, Object value);

    void __set(String prop, Object value);

    void __show(PropId prop, boolean show);

    void __show(String prop, boolean show);

    DraftContext __draftContext();

    Object __resolve();
}
