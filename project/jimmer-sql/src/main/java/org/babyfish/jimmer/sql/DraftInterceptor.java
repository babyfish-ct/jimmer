package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Draft;

@FunctionalInterface
public interface DraftInterceptor<D extends Draft> {

    void beforeSave(D draft, boolean isNew);
}
