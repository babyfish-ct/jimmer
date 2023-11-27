package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.NotApi
import org.babyfish.jimmer.client.meta.common.GetMapping

@NotApi
interface KNotApiService {
    @GetMapping("/not-api")
    fun getNotApi(): String
}