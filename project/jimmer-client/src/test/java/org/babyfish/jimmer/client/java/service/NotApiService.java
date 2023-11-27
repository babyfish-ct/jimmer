package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.IgnoreApi;
import org.babyfish.jimmer.client.meta.common.GetMapping;

@IgnoreApi
public interface NotApiService {
    @GetMapping("/not-api")
    String notApi();
}