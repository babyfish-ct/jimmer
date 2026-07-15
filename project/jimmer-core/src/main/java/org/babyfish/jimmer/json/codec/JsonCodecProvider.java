package org.babyfish.jimmer.json.codec;

import org.jetbrains.annotations.NotNull;

public interface JsonCodecProvider {

    @NotNull
    JsonCodecFamily family();

    @NotNull
    JsonCodec create();
}
