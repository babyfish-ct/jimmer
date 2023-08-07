package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.java.model.Author
import org.babyfish.jimmer.client.kotlin.model.KGender
import org.babyfish.jimmer.client.meta.common.GetMapping
import org.babyfish.jimmer.client.meta.common.RequestParam

interface KEnumService {
    @GetMapping("/enumParam")
    fun enumParam(@RequestParam("gender") gender: KGender)
}