package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Draft;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface DraftInterceptor<D extends Draft> {

    void beforeSave(@NotNull D draft, boolean isNew);
}
