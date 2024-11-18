package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.common.GetMapping
import org.babyfish.jimmer.client.common.PathVariable
import org.babyfish.jimmer.client.kotlin.model.GisArea
import org.babyfish.jimmer.client.kotlin.model.dto.GisAreaView
import org.babyfish.jimmer.client.meta.Api

@Api("gisService")
interface GisService {

    @Api
    @GetMapping("gisArea/{id}")
    fun findAreaById(@PathVariable("id") id: Long): GisAreaView
}