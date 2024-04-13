package org.babyfish.jimmer.sql.kt.model.embedded.p4bug524

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface Point {

    val x: Long

    val y: Long
}