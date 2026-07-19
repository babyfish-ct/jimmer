package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentView2;
import org.babyfish.jimmer.sql.model.hr.dto.EmployeeInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class JacksonTest {

    @Test
    public void testOutput() throws Exception {
        DepartmentView2 department = new DepartmentView2();
        department.setId("00A");
        department.setName("Develop");
        String json = jsonCodec().writer().writeAsString(department);
        Assertions.assertEquals(
                "{\"id\":\"00A\",\"name\":\"Efwfmpq\"}",
                json
        );
        Assertions.assertEquals(
                "DepartmentView2(id=00A, name=Develop)",
                jsonCodec().readerFor(DepartmentView2.class).read(json).toString()
        );
    }

    @Test
    public void testInput() throws Exception {
        EmployeeInput employee = new EmployeeInput();
        employee.setId("001");
        employee.setName("Rossi");
        employee.setGender(Gender.FEMALE);
        String json = jsonCodec().writer().writeAsString(employee);
        Assertions.assertEquals(
                "{\"id\":\"001\",\"gender\":\"FEMALE\",\"name\":\"Spttj\"}",
                json
        );
        Assertions.assertEquals(
                "EmployeeInput(id=001, gender=FEMALE, name=Rossi)",
                jsonCodec().readerFor(EmployeeInput.class).read(json).toString()
        );
    }
}
