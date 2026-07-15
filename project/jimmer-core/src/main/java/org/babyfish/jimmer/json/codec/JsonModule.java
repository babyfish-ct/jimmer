package org.babyfish.jimmer.json.codec;

import org.jetbrains.annotations.Nullable;

public interface JsonModule {

    JsonCodecFamily family();

    @Nullable
    Object createNativeModule();
}
