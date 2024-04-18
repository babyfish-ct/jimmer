package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.common.GetMapping
import org.babyfish.jimmer.client.common.PostMapping
import org.babyfish.jimmer.client.common.RequestParam
import java.util.*

interface KArrayService {
    @GetMapping("/insert/ids")
    fun saveIds(@RequestParam ids: Array<UUID>)
}