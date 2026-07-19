package org.babyfish.jimmer.jackson.codec;

import com.fasterxml.jackson.databind.Module;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JacksonModule;

public interface JsonModule {
    @Nullable
    Module createJacksonModule();

    @Nullable
    JacksonModule createJackson3Module();
}
