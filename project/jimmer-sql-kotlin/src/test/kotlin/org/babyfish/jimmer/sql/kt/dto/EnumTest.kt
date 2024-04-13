package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.enumeration.dto.WriterView
import kotlin.test.Test
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.enumeration.dto.AnotherWriterView
import org.babyfish.jimmer.sql.kt.model.enumeration.dto.ArticleInput

class EnumTest {

    @Test
    fun testWriterView() {
        val view = WriterView(id = 1L, name = "Bob", sex = 100)
        val entity = view.toEntity()
        assertContent(
            """{"id":1,"name":"Bob","gender":"MALE"}""",
            entity.toString()
        )
        val view2 = WriterView(entity)
        assertContent(
            """WriterView(id=1, name=Bob, sex=100)""",
            view2
        )
    }

    @Test
    fun testAnotherWriterView() {
        val view = AnotherWriterView(id = 1L, name = "Bob", sex = null)
        val entity = view.toEntity()
        assertContent(
            """{"id":1,"name":"Bob"}""",
            entity.toString()
        )
        val view2 = AnotherWriterView(entity)
        assertContent(
            """AnotherWriterView(id=1, name=Bob, sex=null)""",
            view2
        )
    }

    @Test
    fun testArticleInput() {
        val input = ArticleInput(
            id = 1,
            name = "Introduce Jimmer",
            writerId = 1,
            writerName = "Bob",
            writerGender = "Male",
            approverId = 2,
            approverName = "Linda",
            approverGender = "Female"
        )
        val entity = input.toEntity()
        assertContent(
            """{
                |--->"id":1,
                |--->"name":"Introduce Jimmer",
                |--->"writer":{
                |--->--->"id":1,
                |--->--->"name":"Bob",
                |--->--->"gender":"MALE"
                |--->},
                |--->"approver":{
                |--->--->"id":2,
                |--->--->"name":"Linda",
                |--->--->"gender":"FEMALE"
                |--->}
                |}""".trimMargin(),
            entity
        )
        val input2 = ArticleInput(entity)
        assertContent(
            """ArticleInput(
                |--->id=1, 
                |--->name=Introduce Jimmer, 
                |--->writerId=1, 
                |--->writerName=Bob, 
                |--->writerGender=Male, 
                |--->approverId=2, 
                |--->approverName=Linda, 
                |--->approverGender=Female
                |)""".trimMargin(),
            input2
        )
    }

    @Test
    fun testArticleInput2() {
        val input = ArticleInput(
            id = 1,
            name = "Introduce Jimmer",
            writerId = 1,
            writerName = "Bob",
            writerGender = null,
            approverId = 2,
            approverName = "Linda",
            approverGender = null
        )
        val entity = input.toEntity()
        assertContent(
            """{
                |--->"id":1,
                |--->"name":"Introduce Jimmer",
                |--->"writer":{
                |--->--->"id":1,
                |--->--->"name":"Bob"
                |--->},
                |--->"approver":{
                |--->--->"id":2,
                |--->--->"name":"Linda",
                |--->--->"gender":null
                |--->}
                |}""".trimMargin(),
            entity
        )
        val input2 = ArticleInput(entity)
        assertContent(
            """ArticleInput(
                |--->id=1, 
                |--->name=Introduce Jimmer, 
                |--->writerId=1, 
                |--->writerName=Bob, 
                |--->writerGender=null, 
                |--->approverId=2, 
                |--->approverName=Linda, 
                |--->approverGender=null
                |)""".trimMargin(),
            input2
        )
    }
}