package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Immutable
interface AssociationInput {

    @get:Min(0)
    val parentId: @Max(100) Long

    val childIds: List<Long>
}