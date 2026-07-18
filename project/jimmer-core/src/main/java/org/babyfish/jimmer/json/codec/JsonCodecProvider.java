package org.babyfish.jimmer.json.codec;

import org.jetbrains.annotations.NotNull;

public interface JsonCodecProvider {

    int priority();

    /**
     * 返回当前 Provider 提供的 JSON 编解码门面。
     */
    @NotNull
    JsonCodec codec();
}
