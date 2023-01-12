package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.pojo.Static
import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Entity
@StaticType(alias = "default", topLevelName = "BookInput")
@StaticType(alias = "composite", topLevelName = "CompositeBookInput")
interface Book {

    @Id
    val id: Long

    @Key
    val name: @NotBlank String

    @Key
    val edition: @Positive Int

    val price: @Positive BigDecimal

    @ManyToOne
    @Static(alias = "default", name = "storeId", idOnly = true)
    @Static(alias = "composite")
    val store: BookStore?

    @ManyToMany
    @Static(alias = "default", name = "authorIds", idOnly = true)
    @Static(alias = "composite")
    val authors: List<Author>
}