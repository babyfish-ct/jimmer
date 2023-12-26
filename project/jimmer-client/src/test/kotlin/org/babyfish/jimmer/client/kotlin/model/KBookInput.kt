package org.babyfish.jimmer.client.kotlin.model

import java.math.BigDecimal

class KBookInput(
    private val name: String,
    private val edition: Int,
    private val price: BigDecimal,
    /**
     * Null is allowed
     */
    private val storeId: Long?,
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
