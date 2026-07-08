package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.common.GetMapping
import org.babyfish.jimmer.client.kotlin.model.dto.KClientView
import org.babyfish.jimmer.client.meta.Api

@Api("kPolymorphicDtoService")
interface KPolymorphicDtoService {

    @Api
    @GetMapping("/k-clients")
    fun getClients(): List<KClientView>

    @Api
    @GetMapping("/k-manual-views")
    fun getManualViews(): List<ManualView>

    interface ManualView {

        val name: String

        class Branch(
            override val name: String
        ) : ManualView
    }
}
