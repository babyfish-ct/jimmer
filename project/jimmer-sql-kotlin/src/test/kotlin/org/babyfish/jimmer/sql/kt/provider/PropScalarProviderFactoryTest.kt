package org.babyfish.jimmer.sql.kt.provider

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.provider.TagsScalarProvider
import org.babyfish.jimmer.sql.kt.model.provider.Topic
import org.babyfish.jimmer.sql.kt.model.provider.TopicProps
import kotlin.test.Test

class PropScalarProviderFactoryTest : AbstractQueryTest() {

    @Test
    fun testQuery() {
        executeAndExpect(
            sqlClient {
                addPropScalarProviderFactory {
                    if (TopicProps.TAGS.match(it)) {
                        TagsScalarProvider()
                    } else {
                        null
                    }
                }
            }.createQuery(Topic::class) {
                select(table)
            }
        ) {
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.tags_mask from TOPIC tb_1_")
            rows(
                """[{"id":1,"name":"What is the best ORM","tags":["QUESTION","PUBLIC"]}]"""
            )
        }
    }
}