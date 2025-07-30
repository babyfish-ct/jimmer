package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.constant
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.plus
import org.babyfish.jimmer.sql.kt.ast.query.cteBaseTableSymbol
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import kotlin.test.Test

class RecursiveBaseQueryTest : AbstractQueryTest() {

    @Test
    fun test() {
        val baseTable = cteBaseTableSymbol {
            sqlClient.createBaseQuery(TreeNode::class) {
                where(table.parentId.isNull())
                selections
                    .add(table)
                    .add(constant(1))
            }.unionAllRecursively {
                sqlClient.createBaseQuery(TreeNode::class, it, {
                    source.parentId eq target._1.id
                }) {
                    selections
                        .add(table)
                        .add(recursive._2 + 1)
                }
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                orderBy(table._2, table._1.name)
                select(
                    table._1.fetchBy {
                        name()
                    },
                    table._2
                )
            }
        ) {
            sql(
                """with tb_1_(c1, c2, c3) as (
                    |--->select tb_2_.NODE_ID, tb_2_.NAME, 1 
                    |--->from TREE_NODE tb_2_ 
                    |--->where tb_2_.PARENT_ID is null 
                    |--->union all 
                    |--->select tb_4_.NODE_ID, tb_4_.NAME, tb_1_.c3 + ? 
                    |--->from TREE_NODE tb_4_ 
                    |--->inner join tb_1_ on tb_4_.PARENT_ID = tb_1_.c1
                    |) 
                    |select tb_1_.c1, tb_1_.c2, tb_1_.c3 
                    |from tb_1_ 
                    |order by tb_1_.c3 asc, tb_1_.c2 asc""".trimMargin()
            )
            rows(
                """[
                    |{"_1":{"id":1,"name":"Home"},"_2":1},
                    |
                    |{"_1":{"id":9,"name":"Clothing"},"_2":2},
                    |{"_1":{"id":2,"name":"Food"},"_2":2},
                    |
                    |{"_1":{"id":6,"name":"Bread"},"_2":3},
                    |{"_1":{"id":3,"name":"Drinks"},"_2":3},
                    |{"_1":{"id":18,"name":"Man"},"_2":3},
                    |{"_1":{"id":10,"name":"Woman"},"_2":3},
                    |
                    |{"_1":{"id":7,"name":"Baguette"},"_2":4},
                    |{"_1":{"id":19,"name":"Casual wear"},"_2":4},
                    |{"_1":{"id":11,"name":"Casual wear"},"_2":4},
                    |{"_1":{"id":8,"name":"Ciabatta"},"_2":4},
                    |{"_1":{"id":4,"name":"Coca Cola"},"_2":4},
                    |{"_1":{"id":5,"name":"Fanta"},"_2":4},
                    |{"_1":{"id":22,"name":"Formal wear"},"_2":4},
                    |{"_1":{"id":15,"name":"Formal wear"},"_2":4},
                    |
                    |{"_1":{"id":12,"name":"Dress"},"_2":5},
                    |{"_1":{"id":20,"name":"Jacket"},"_2":5},
                    |{"_1":{"id":21,"name":"Jeans"},"_2":5},
                    |{"_1":{"id":14,"name":"Jeans"},"_2":5},
                    |{"_1":{"id":13,"name":"Miniskirt"},"_2":5},
                    |{"_1":{"id":24,"name":"Shirt"},"_2":5},
                    |{"_1":{"id":17,"name":"Shirt"},"_2":5},
                    |{"_1":{"id":23,"name":"Suit"},"_2":5},
                    |{"_1":{"id":16,"name":"Suit"},"_2":5}
                    |]""".trimMargin()
            )
        }
    }
}