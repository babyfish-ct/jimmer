package org.babyfish.jimmer.json.codec;

import org.jetbrains.annotations.NotNull;

public interface JsonCodecProvider {

    int priority();

    @NotNull
    JsonCodec create();
}
