package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.inheritance.dto.AdministratorInputForIssue684
import org.junit.Test
import kotlin.test.expect

class AdministratorInputForIssue684Test {

    @Test
    fun testNull() {
        expect(
            "{}"
        ) {
            AdministratorInputForIssue684().toEntity().toString()
        }
    }

    @Test
    fun testNonNull() {
        expect(
            "{\"metadata\":{\"name\":\"Metadata\"}}"
        ) {
            AdministratorInputForIssue684(
                metadata = AdministratorInputForIssue684.TargetOf_metadata("Metadata")
            ).toEntity().toString()
        }
    }
}