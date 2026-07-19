package org.babyfish.jimmer.jackson.codec;

import org.jetbrains.annotations.NotNull;

public interface JsonCodecProvider {
    @NotNull
    JsonCodec<?> create();
}
