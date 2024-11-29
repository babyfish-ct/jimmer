package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.babyfish.jimmer.sql.kt.model.hr.dto.DepartmentView2
import org.babyfish.jimmer.sql.kt.model.hr.dto.EmployeeInput
import kotlin.test.Test
import kotlin.test.expect

class JacksonTest {

    @Test
    fun testOutput() {
        testOutputImpl(false)
        testOutputImpl(true)
    }

    @Test
    fun testInputForIssue807() {
        testInputForIssue807Impl(false)
        testInputForIssue807Impl(true)
    }

    private fun testOutputImpl(registerKtMode: Boolean) {
        val mapper = ObjectMapper().apply {
            if (registerKtMode) {
                registerModule(KotlinModule.Builder().build())
            }
        }
        val department = DepartmentView2(id = "00A", name = "Develop")
        val json = mapper.writeValueAsString(department)
        expect(
            "{\"id\":\"00A\",\"name\":\"Efwfmpq\"}"
        ) {
            json
        }
        expect(
            "DepartmentView2(id=00A, name=Develop)"
        ) {
            mapper.readValue(json, DepartmentView2::class.java).toString()
        }
    }

    private fun testInputForIssue807Impl(registerKtMode: Boolean) {
        val mapper = ObjectMapper().apply {
            if (registerKtMode) {
                registerModule(KotlinModule.Builder().build())
            }
        }
        val employee = EmployeeInput("001", "Rossi")
        val json = mapper.writeValueAsString(employee)
        expect(
            "{\"id\":\"001\",\"name\":\"Spttj\"}"
        ) {
            json
        }
        expect(
            "EmployeeInput(id=001, name=Rossi)"
        ) {
            mapper.readValue(json, EmployeeInput::class.java).toString()
        }
    }
}