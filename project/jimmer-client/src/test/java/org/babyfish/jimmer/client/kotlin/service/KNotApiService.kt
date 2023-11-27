package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.IgnoreApi
import org.babyfish.jimmer.client.meta.common.GetMapping

@IgnoreApi
interface KNotApiService {
    @GetMapping("/not-api")
    fun getNotApi(): String
}