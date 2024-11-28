package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.common.GetMapping
import org.babyfish.jimmer.client.common.PathVariable
import org.babyfish.jimmer.client.kotlin.model.GisArea
import org.babyfish.jimmer.client.kotlin.model.by
import org.babyfish.jimmer.client.meta.Api
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher

@Api("gisService")
interface GisService {

    @Api
    @GetMapping("gisArea/{id}")
    fun findAreaById(@PathVariable("id") id: Long): @FetchBy("FETCHER") GisArea

    companion object {
        val FETCHER = newFetcher(GisArea::class).by {
            allScalarFields()
        }
    }
}