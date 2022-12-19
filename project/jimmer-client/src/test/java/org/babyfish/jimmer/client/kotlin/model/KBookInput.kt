package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.client.Doc
import org.babyfish.jimmer.client.ExportFields
import java.math.BigDecimal

@ExportFields
class KBookInput(
    private val name: String,
    private val edition: Int,
    private val price: BigDecimal,
    @Doc("Null is allowed") private val storeId: Long?,
    private val authorIds: List<Long>
) {
    override fun toString(): String {
        return "BookInput{" +
            "name='" + name + '\'' +
            ", edition=" + edition +
            ", price=" + price +
            ", storeId=" + storeId +
            ", authorIds=" + authorIds +
            '}'
    }
}
