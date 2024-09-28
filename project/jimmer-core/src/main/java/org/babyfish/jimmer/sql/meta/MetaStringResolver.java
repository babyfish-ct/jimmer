package org.babyfish.jimmer.sql.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface MetaStringResolver {

    MetaStringResolver NO_OP = str -> str;

    @Nullable
    String resolve(@NotNull String value);
}
