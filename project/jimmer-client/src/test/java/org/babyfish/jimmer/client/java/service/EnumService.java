package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.java.model.Gender;
import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.common.RequestParam;

public interface EnumService {
    @GetMapping("/enumParam")
    void enumParam(@RequestParam("gender") Gender gender);
}
