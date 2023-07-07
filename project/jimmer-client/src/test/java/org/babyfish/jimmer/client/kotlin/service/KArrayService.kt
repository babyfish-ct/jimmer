package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.meta.common.GetMapping
import org.babyfish.jimmer.client.meta.common.RequestParam
import java.util.*

interface KArrayService {
    @GetMapping("/insert/ids")
    fun saveIds(@RequestParam ids: Array<UUID>)
}