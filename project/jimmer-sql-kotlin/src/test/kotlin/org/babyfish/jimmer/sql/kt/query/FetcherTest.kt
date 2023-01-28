package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.table.isNull
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import org.junit.Test

class FetcherTest : AbstractQueryTest() {

    @Test
    fun testWithoutFilter() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.id eq 1L)
                select(
                    table.fetchBy {
                        allScalarFields()
                        store {
                            allScalarFields()
                        }
                        authors {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |where tb_1_.ID = ?""".trimMargin()
            )
            variables(1L)
            statement(1).apply {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                        |from BOOK_STORE as tb_1_ 
                        |where tb_1_.ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(2).apply {
                sql(
                    """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER 
                        |from AUTHOR as tb_1_ 
                        |inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID 
                        |where tb_2_.BOOK_ID = ?""".trimMargin()
                )
                variables(1L)
            }
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"Learning GraphQL",
                    |--->--->"edition":1,
                    |--->--->"price":50.00,
                    |--->--->"store":{
                    |--->--->--->"id":1,
                    |--->--->--->"name":"O'REILLY",
                    |--->--->--->"version":0,
                    |--->--->--->"website":null
                    |--->--->},
                    |--->--->"authors":[
                    |--->--->--->{
                    |--->--->--->--->"id":1,
                    |--->--->--->--->"firstName":"Eve",
                    |--->--->--->--->"lastName":"Procello",
                    |--->--->--->--->"gender":"FEMALE"
                    |--->--->--->},{
                    |--->--->--->--->"id":2,
                    |--->--->--->--->"firstName":"Alex",
                    |--->--->--->--->"lastName":"Banks",
                    |--->--->--->--->"gender":"MALE"
                    |--->--->--->}
                    |--->--->]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testWithFilter() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.id eq 1L)
                select(
                    table.fetchBy {
                        allScalarFields()
                        store {
                            allScalarFields()
                        }
                        authors {
                            allScalarFields()
                        }
                    },
                    table.fetchBy {
                        allScalarFields()
                        store {
                            allScalarFields()
                        }
                        authors({
                            filter {
                                where(table.firstName eq "Alex")
                            }
                        }) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select 
                    |tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, 
                    |tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |where tb_1_.ID = ?""".trimMargin()
            )
            variables(1L)
            statement(1).apply {
                sql(
                    """select 
                        |tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                        |from BOOK_STORE as tb_1_ 
                        |where tb_1_.ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(2).apply {
                sql(
                    """select 
                        |tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER 
                        |from AUTHOR as tb_1_ 
                        |inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID 
                        |where tb_2_.BOOK_ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(3).apply {
                sql(
                    """select 
                        |tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                        |from BOOK_STORE as tb_1_ 
                        |where tb_1_.ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(4).apply {
                sql(
                    """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER 
                        |from AUTHOR as tb_1_ 
                        |inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID 
                        |where 
                        |--->tb_2_.BOOK_ID = ? 
                        |and 
                        |--->tb_1_.FIRST_NAME = ?""".trimMargin()
                )
                variables(1L, "Alex")
            }
            rows {
                contentEquals(
                    """[
                        |--->Tuple2(
                        |--->--->_1={
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"store":{
                        |--->--->--->--->"id":1,
                        |--->--->--->--->"name":"O'REILLY",
                        |--->--->--->--->"version":0,
                        |--->--->--->--->"website":null
                        |--->--->--->},
                        |--->--->--->"authors":[
                        |--->--->--->--->{
                        |--->--->--->--->--->"id":1,
                        |--->--->--->--->--->"firstName":"Eve",
                        |--->--->--->--->--->"lastName":"Procello",
                        |--->--->--->--->--->"gender":"FEMALE"
                        |--->--->--->--->},{
                        |--->--->--->--->--->"id":2,
                        |--->--->--->--->--->"firstName":"Alex",
                        |--->--->--->--->--->"lastName":"Banks",
                        |--->--->--->--->--->"gender":"MALE"
                        |--->--->--->--->}
                        |--->--->--->]
                        |--->--->}, 
                        |--->--->_2={
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"store":{
                        |--->--->--->--->"id":1,
                        |--->--->--->--->"name":"O'REILLY",
                        |--->--->--->--->"version":0,
                        |--->--->--->--->"website":null
                        |--->--->--->},
                        |--->--->--->"authors":[
                        |--->--->--->--->{
                        |--->--->--->--->--->"id":2,
                        |--->--->--->--->--->"firstName":"Alex",
                        |--->--->--->--->--->"lastName":"Banks",
                        |--->--->--->--->--->"gender":"MALE"
                        |--->--->--->--->}
                        |--->--->--->]
                        |--->--->}
                        |--->)
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun testFetchTwoLayers() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(table.parent.isNull())
                select(
                    table.fetchBy {
                        allScalarFields()
                        childNodes({
                            depth(2)
                        }) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.NODE_ID, tb_1_.NAME 
                    |from TREE_NODE as tb_1_ 
                    |where tb_1_.PARENT_ID is null""".trimMargin()
            )
            statement(1).apply {
                sql(
                    """select tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(2).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?)""".trimMargin()
                )
                variables(setOf(2L, 9L))
            }
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"Home",
                    |--->--->"childNodes":[
                    |--->--->--->{
                    |--->--->--->--->"id":9,
                    |--->--->--->--->"name":"Clothing",
                    |--->--->--->--->"childNodes":[
                    |--->--->--->--->--->{"id":18,"name":"Man"},
                    |--->--->--->--->--->{"id":10,"name":"Woman"}
                    |--->--->--->--->]
                    |--->--->--->},{
                    |--->--->--->--->"id":2,
                    |--->--->--->--->"name":"Food",
                    |--->--->--->--->"childNodes":[
                    |--->--->--->--->--->{"id":6,"name":"Bread"},
                    |--->--->--->--->--->{"id":3,"name":"Drinks"}
                    |--->--->--->--->]
                    |--->--->--->}
                    |--->--->]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testRecursive() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(table.parent.isNull())
                select(
                    table.fetchBy {
                        allScalarFields()
                        childNodes({
                            recursive()
                        }) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.NODE_ID, tb_1_.NAME 
                    |from TREE_NODE as tb_1_ 
                    |where tb_1_.PARENT_ID is null""".trimMargin()
            )
            statement(1).apply {
                sql(
                    """select tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(2).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?)""".trimMargin()
                )
                variables(setOf(2L, 9L))
            }
            statement(3).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?, ?, ?)""".trimMargin()
                )
                variables(setOf(3L, 6L, 10L, 18L))
            }
            statement(4).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)""".trimMargin()
                )
                variables(setOf(4L, 5L, 7L, 8L, 11L, 15L, 19L, 22L))
            }
            statement(5).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimMargin()
                )
                variables(setOf(12L, 13L, 14L, 16L, 17L, 20L, 21L, 23, 24L))
            }
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"Home",
                    |--->--->"childNodes":[
                    |--->--->--->{
                    |--->--->--->--->"id":9,
                    |--->--->--->--->"name":"Clothing",
                    |--->--->--->--->"childNodes":[
                    |--->--->--->--->--->{
                    |--->--->--->--->--->--->"id":18,
                    |--->--->--->--->--->--->"name":"Man",
                    |--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->"id":19,
                    |--->--->--->--->--->--->--->--->"name":"Casual wear",
                    |--->--->--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->--->--->"id":20,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Jacket",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->--->--->"id":21,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Jeans",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->--->--->]
                    |--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->"id":22,
                    |--->--->--->--->--->--->--->--->"name":"Formal wear",
                    |--->--->--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->--->--->"id":24,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Shirt",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->--->--->"id":23,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Suit",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->--->--->]
                    |--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->]
                    |--->--->--->--->--->},{
                    |--->--->--->--->--->--->"id":10,
                    |--->--->--->--->--->--->"name":"Woman",
                    |--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->"id":11,
                    |--->--->--->--->--->--->--->--->"name":"Casual wear",
                    |--->--->--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->--->--->"id":12,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Dress",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->--->--->"id":14,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Jeans",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->--->--->"id":13,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Miniskirt",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->--->--->]
                    |--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->"id":15,
                    |--->--->--->--->--->--->--->--->"name":"Formal wear",
                    |--->--->--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->--->--->"id":17,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Shirt",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->--->--->"id":16,
                    |--->--->--->--->--->--->--->--->--->--->"name":"Suit",
                    |--->--->--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->--->--->]
                    |--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->]
                    |--->--->--->--->--->}
                    |--->--->--->--->]
                    |--->--->--->},{
                    |--->--->--->--->"id":2,
                    |--->--->--->--->"name":"Food",
                    |--->--->--->--->"childNodes":[
                    |--->--->--->--->--->{
                    |--->--->--->--->--->--->"id":6,
                    |--->--->--->--->--->--->"name":"Bread",
                    |--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->"id":7,
                    |--->--->--->--->--->--->--->--->"name":"Baguette",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->"id":8,
                    |--->--->--->--->--->--->--->--->"name":"Ciabatta",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->]
                    |--->--->--->--->--->},{
                    |--->--->--->--->--->--->"id":3,
                    |--->--->--->--->--->--->"name":"Drinks",
                    |--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->"id":4,
                    |--->--->--->--->--->--->--->--->"name":"Coca Cola",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->"id":5,
                    |--->--->--->--->--->--->--->--->"name":"Fanta",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->]
                    |--->--->--->--->--->}
                    |--->--->--->--->]
                    |--->--->--->}
                    |--->--->]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testRecursiveExceptClothing() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(table.parent.isNull())
                select(
                    table.fetchBy {
                        allScalarFields()
                        childNodes({
                            recursive {
                                entity.name != "Clothing"
                            }
                        }) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.NODE_ID, tb_1_.NAME 
                    |from TREE_NODE as tb_1_ 
                    |where tb_1_.PARENT_ID is null""".trimMargin()
            )
            statement(1).apply {
                sql(
                    """select tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID = ?""".trimMargin()
                )
                variables(1L)
            }
            statement(2).apply {
                sql(
                    """select 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID = ?""".trimMargin()
                )
                variables(setOf(2L))
            }
            statement(3).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?)""".trimMargin()
                )
                variables(setOf(3L, 6L))
            }
            statement(4).apply {
                sql(
                    """select 
                        |--->tb_1_.PARENT_ID, 
                        |--->tb_1_.NODE_ID, tb_1_.NAME 
                        |from TREE_NODE as tb_1_ 
                        |where tb_1_.PARENT_ID in (?, ?, ?, ?)""".trimMargin()
                )
                variables(setOf(4L, 5L, 7L, 8L))
            }
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"Home",
                    |--->--->"childNodes":[
                    |--->--->--->{
                    |--->--->--->--->"id":9,
                    |--->--->--->--->"name":"Clothing"
                    |--->--->--->},{
                    |--->--->--->--->"id":2,
                    |--->--->--->--->"name":"Food",
                    |--->--->--->--->"childNodes":[
                    |--->--->--->--->--->{
                    |--->--->--->--->--->--->"id":6,
                    |--->--->--->--->--->--->"name":"Bread",
                    |--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->"id":7,
                    |--->--->--->--->--->--->--->--->"name":"Baguette",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->"id":8,
                    |--->--->--->--->--->--->--->--->"name":"Ciabatta",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->]
                    |--->--->--->--->--->},{
                    |--->--->--->--->--->--->"id":3,
                    |--->--->--->--->--->--->"name":"Drinks",
                    |--->--->--->--->--->--->"childNodes":[
                    |--->--->--->--->--->--->--->{
                    |--->--->--->--->--->--->--->--->"id":4,
                    |--->--->--->--->--->--->--->--->"name":"Coca Cola",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->},{
                    |--->--->--->--->--->--->--->--->"id":5,
                    |--->--->--->--->--->--->--->--->"name":"Fanta",
                    |--->--->--->--->--->--->--->--->"childNodes":[]
                    |--->--->--->--->--->--->--->}
                    |--->--->--->--->--->--->]
                    |--->--->--->--->--->}
                    |--->--->--->--->]
                    |--->--->--->}
                    |--->--->]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testCalculation() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(table.fetchBy {
                    allScalarFields()
                    avgPrice()
                })
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE from BOOK_STORE as tb_1_"""
            )
            statement(1).sql(
                """select tb_1_.STORE_ID, coalesce(avg(tb_1_.PRICE), ?) 
                    |from BOOK as tb_1_ 
                    |where tb_1_.STORE_ID in (?, ?) 
                    |group by tb_1_.STORE_ID""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"O'REILLY",
                    |--->--->"version":0,
                    |--->--->"avgPrice":58.500000000000,
                    |--->--->"website":null
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"MANNING",
                    |--->--->"version":0,
                    |--->--->"avgPrice":80.333333333333,
                    |--->--->"website":null
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testBookInput() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.id eq 12L)
                select(table.fetch(BookInput::class))
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |where tb_1_.ID = ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.AUTHOR_ID 
                    |from BOOK_AUTHOR_MAPPING as tb_1_ 
                    |where tb_1_.BOOK_ID = ?""".trimMargin()
            )
            rows {
                contentEquals(
                    """BookInput(
                        |--->id=12, 
                        |--->name=GraphQL in Action, 
                        |--->edition=3, 
                        |--->price=80.00, 
                        |--->storeId=2, 
                        |--->authorIds=[5]
                        |)""".trimMargin(),
                    it[0].toString()
                )
            }
        }
    }

    @Test
    fun testCompositeBookInput() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.id eq 12L)
                select(table.fetch(CompositeBookInput::class))
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |where tb_1_.ID = ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE as tb_1_ 
                    |where tb_1_.ID = ?""".trimMargin()
            )
            statement(2).sql(
                """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER 
                    |from AUTHOR as tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID 
                    |where tb_2_.BOOK_ID = ?""".trimMargin()
            )
            rows {
                contentEquals(
                    """CompositeBookInput(
                        |--->id=12, 
                        |--->name=GraphQL in Action, 
                        |--->edition=3, 
                        |--->price=80.00, 
                        |--->store=TargetOf_store(
                        |--->--->id=2, 
                        |--->--->name=MANNING, 
                        |--->--->version=0, 
                        |--->--->website=null
                        |--->), 
                        |--->authors=[
                        |--->--->TargetOf_authors(
                        |--->--->--->id=5, 
                        |--->--->--->firstName=Samer, 
                        |--->--->--->lastName=Buna, 
                        |--->--->--->gender=MALE
                        |--->--->)
                        |--->]
                        |)""".trimMargin(),
                    it[0].toString()
                )
            }
        }
    }
}