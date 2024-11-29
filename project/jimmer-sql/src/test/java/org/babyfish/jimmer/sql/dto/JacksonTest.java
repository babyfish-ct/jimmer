package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentView2;
import org.babyfish.jimmer.sql.model.hr.dto.EmployeeInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JacksonTest {

    @Test
    public void testOutput() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        DepartmentView2 department = new DepartmentView2();
        department.setId("00A");
        department.setName("Develop");
        String json = mapper.writeValueAsString(department);
        Assertions.assertEquals(
                "{\"id\":\"00A\",\"name\":\"Efwfmpq\"}",
                json
        );
        Assertions.assertEquals(
                "DepartmentView2(id=00A, name=Develop)",
                mapper.readValue(json, DepartmentView2.class).toString()
        );
    }

    @Test
    public void testInput() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        EmployeeInput employee = new EmployeeInput();
        employee.setId("001");
        employee.setName("Rossi");
        employee.setGender(Gender.FEMALE);
        String json = mapper.writeValueAsString(employee);
        Assertions.assertEquals(
                "{\"id\":\"001\",\"gender\":\"FEMALE\",\"name\":\"Spttj\"}",
                json
        );
        Assertions.assertEquals(
                "EmployeeInput(id=001, gender=FEMALE, name=Rossi)",
                mapper.readValue(json, EmployeeInput.class).toString()
        );
    }
}
