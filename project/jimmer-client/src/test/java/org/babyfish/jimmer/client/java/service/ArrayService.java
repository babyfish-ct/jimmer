package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.meta.common.GetMapping;
import org.babyfish.jimmer.client.meta.common.RequestParam;

import java.util.UUID;

public interface ArrayService {
    @GetMapping("/insert/ids")
    void saveIds(@RequestParam UUID[] ids);
}
