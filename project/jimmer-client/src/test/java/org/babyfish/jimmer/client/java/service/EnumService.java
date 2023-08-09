package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.java.model.Author;
import org.babyfish.jimmer.client.java.model.Gender;
import org.babyfish.jimmer.client.meta.common.GetMapping;
import org.babyfish.jimmer.client.meta.common.RequestParam;

import java.util.List;

public interface EnumService {
    @GetMapping("/enumParam")
    void enumParam(@RequestParam("gender") Gender gender);
}
