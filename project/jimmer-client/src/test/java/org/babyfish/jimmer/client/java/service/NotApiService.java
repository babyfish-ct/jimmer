package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.NotApi;
import org.babyfish.jimmer.client.meta.common.GetMapping;

@NotApi
public interface NotApiService {
    @GetMapping("/not-api")
    String notApi();
}