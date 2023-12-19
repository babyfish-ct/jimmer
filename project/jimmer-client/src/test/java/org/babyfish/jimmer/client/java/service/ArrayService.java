package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.common.RequestParam;

import java.util.UUID;

public interface ArrayService {
    @GetMapping("/insert/ids")
    void saveIds(@RequestParam UUID[] ids);
}
