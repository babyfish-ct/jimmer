package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.kotlin.model.KBookStore
import org.babyfish.jimmer.client.meta.common.GetMapping

interface KBookStoreService {

    @GetMapping("/bookStores")
    fun findDefaultBookStores(): List<KBookStore>
}