package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.OldChain;
import org.jetbrains.annotations.NotNull;

public interface NativeContext {

    @OldChain
    @NotNull
    NativeContext expression(@NotNull Expression<?> expression);

    @OldChain
    @NotNull
    NativeContext value(@NotNull Object value);
}
