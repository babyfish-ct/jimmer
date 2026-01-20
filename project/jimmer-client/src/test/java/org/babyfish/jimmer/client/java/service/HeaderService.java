package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.common.RequestHeader;
import org.babyfish.jimmer.client.meta.Api;
import org.jetbrains.annotations.Nullable;

@Api("headerService")
public interface HeaderService {

    @Api
    @GetMapping("/headers")
    void headers(
            @RequestHeader("Access-Token") String accessToken,
            @Nullable @RequestHeader(value = "Optional-Token", required = false) String optionalToken
    );
}

