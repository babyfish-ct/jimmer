package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.kotlin.model.KGender
import org.babyfish.jimmer.client.common.GetMapping
import org.babyfish.jimmer.client.common.RequestParam

interface KEnumService {
    @GetMapping("/enumParam")
    fun enumParam(@RequestParam("gender") gender: KGender)
}