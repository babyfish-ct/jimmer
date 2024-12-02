package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.inheritance.dto.AdministratorInputForIssue684
import org.babyfish.jimmer.sql.kt.model.inheritance.dto.AdministratorInputForIssue819
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.expect

class AdministratorInputTest {

    @Test
    fun testNullForIssue684() {
        expect(
            "{}"
        ) {
            AdministratorInputForIssue684().toEntity().toString()
        }
    }

    @Test
    fun testNonNullForIssue684() {
        expect(
            "{\"metadata\":{\"name\":\"Metadata\"}}"
        ) {
            AdministratorInputForIssue684(
                metadata = AdministratorInputForIssue684.TargetOf_metadata("Metadata")
            ).toEntity().toString()
        }
    }

    @Test
    fun testIssueFor819() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val mapper = ObjectMapper().registerModule(JavaTimeModule())
        val input = AdministratorInputForIssue819(
            id = 10L,
            name = "SuperAdmin",
            createdTime = LocalDateTime.parse("2024-12-02 13:07:24", formatter),
            modifiedTime = LocalDateTime.parse("2024-12-03 02:00:14", formatter),
        )
        val json = mapper.writeValueAsString(input)
        assertContent(
            """{"
                |--->id":10,
                |--->"name":"SuperAdmin",
                |--->"createdTime":"2024/12/02 13-07-24",
                |--->"modifiedTime":"2024/12/03 02-00-14"
                |}""".trimMargin(),
            json
        )
        val input2 = mapper.readValue(json, AdministratorInputForIssue819::class.java)
        assertContent(
            """AdministratorInputForIssue819(
                |--->id=10, 
                |--->name=SuperAdmin, 
                |--->createdTime=2024-12-02T13:07:24, 
                |--->modifiedTime=2024-12-03T02:00:14)""".trimMargin(),
            input2
        )
    }
}