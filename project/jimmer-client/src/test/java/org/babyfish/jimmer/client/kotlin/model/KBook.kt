package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToOne
import java.math.BigDecimal

@Entity
interface KBook {

    @Id
    val id: Long

    val name: String?

    val edition: Int

    val price: BigDecimal?

    /**
     * The bookstore to which the current book belongs, null is allowed
     */
    @ManyToOne
    val store: KBookStore?

    /**
     * All authors involved in writing the work
     */
    @ManyToMany
    val authors: List<KAuthor>
}
