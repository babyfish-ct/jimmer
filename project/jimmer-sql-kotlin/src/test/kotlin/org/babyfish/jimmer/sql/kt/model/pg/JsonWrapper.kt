package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "pg_json_wrapper")
interface JsonWrapper {

    @Id
    val id: Long

    @Column(name = "json_1")
    val point: Point

    @Column(name = "json_2")
    val tags: List<String>

    @Column(name = "json_3")
    val scores: Map<Long, Int>
}