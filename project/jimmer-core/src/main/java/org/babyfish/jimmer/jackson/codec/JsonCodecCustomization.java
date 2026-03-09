package org.babyfish.jimmer.jackson.codec;

public interface JsonCodecCustomization {
    void customizeV2(com.fasterxml.jackson.databind.cfg.MapperBuilder<?, ?> builder);

    void customizeV3(tools.jackson.databind.cfg.MapperBuilder<?, ?> builder);
}
