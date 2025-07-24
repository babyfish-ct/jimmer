package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.constant
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.plus
import org.babyfish.jimmer.sql.kt.ast.query.unionAllRecursively
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import kotlin.test.Test

class RecursiveBaseQueryTest : AbstractQueryTest() {

    @Test
    fun test() {
        val baseTable = unionAllRecursively(
            sqlClient.createBaseQuery(TreeNode::class) {
                where(table.parentId.isNull())
                selector()
                    .add(table)
                    .add(constant(1))
            }
        ) {
            sqlClient.createBaseQuery(TreeNode::class, it, {
                source.parentId eq target._1.id
            }) {
                where(table.parentId.isNull())
                selector()
                    .add(table)
                    .add(recursive._2 + 1)
            }
        }.asCteBaseTable()
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                orderBy(table._2, table._1.name)
                select(table._1.fetchBy {
                    name()
                })
            }
        ) {

        }
    }
}