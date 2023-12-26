package org.babyfish.jimmer.client.kotlin.model

import com.fasterxml.jackson.annotation.JsonValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface KBookStore {

    @Id
    val id: Long

    val name: String?

    val coordinate: KCoordinate

    val level: KLevel

    /**
     * All books available in this bookstore
     */
    @OneToMany(mappedBy = "store")
    val books: List<KBook>
}

enum class KLevel {
    LOW,
    MIDDLE,
    HIGH;

    @JsonValue
    fun toInt(): Int =
        when (this) {
            LOW -> 10
            MIDDLE -> 20
            HIGH -> 30
        }
}