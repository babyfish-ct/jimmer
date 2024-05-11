package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.kotlin.model.KBookStore
import org.babyfish.jimmer.client.common.GetMapping
import org.babyfish.jimmer.client.common.PutMapping
import org.babyfish.jimmer.client.common.RequestBody
import org.babyfish.jimmer.client.meta.Api

@Api("kBookStoreService")
interface KBookStoreService {

    @Api
    @GetMapping("/bookStores")
    fun findDefaultBookStores(): List<KBookStore>

    @Api
    @PutMapping("/bookStore")
    fun saveBookForIssue554(@RequestBody input: BookStoreInput)

    data class BookStoreInput(
        val id: Long,
        val name: String
    )
}