package org.babyfish.jimmer.benchmark.ktorm

import org.ktorm.entity.Entity

interface KtormData : Entity<KtormData> {
    companion object : Entity.Factory<KtormData>()
    val id: Long
    var value1: Int
    var value2: Int
    var value3: Int
    var value4: Int
    var value5: Int
    var value6: Int
    var value7: Int
    var value8: Int
    var value9: Int
}