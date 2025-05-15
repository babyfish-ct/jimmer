package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.dto.BookInput3;
import org.babyfish.jimmer.sql.model.dto.TreeNodeInput2;
import org.babyfish.jimmer.sql.model.hr.dto.EmployeeInput;
import org.junit.jupiter.api.Test;

public class Issue1043Test extends Tests {

    @Test
    public void testEmployeeWithoutId() {
        EmployeeInput input = new EmployeeInput();
        input.setName("Tom");
        input.setGender(Gender.MALE);
        assertContentEquals(
                "{\"name\":\"Tom\",\"gender\":\"MALE\"}",
                input.toEntity()
        );
    }

    @Test
    public void testEmployeeWithInternalId() {
        EmployeeInput input = new EmployeeInput();
        input.setId("17");
        input.setName("Tom");
        input.setGender(Gender.MALE);
        assertContentEquals(
                "{\"id\":\"17\",\"name\":\"Tom\",\"gender\":\"MALE\"}",
                input.toEntity()
        );
    }

    @Test
    public void testEmployeeWithExternalId() {
        EmployeeInput input = new EmployeeInput();
        input.setName("Tom");
        input.setGender(Gender.MALE);
        assertContentEquals(
                "{\"id\":\"19\",\"name\":\"Tom\",\"gender\":\"MALE\"}",
                input.toEntityById(19L)
        );
    }

    @Test
    public void testEmployeeWithOverrideId() {
        EmployeeInput input = new EmployeeInput();
        input.setId("17");
        input.setName("Tom");
        input.setGender(Gender.MALE);
        assertContentEquals(
                "{\"id\":\"19\",\"name\":\"Tom\",\"gender\":\"MALE\"}",
                input.toEntityById(19L)
        );
    }

    @Test
    public void testTreeNodeWithoutId() {
        TreeNodeInput2 input = new TreeNodeInput2();
        input.setName("Root");
        assertContentEquals(
                "{\"name\":\"Root\"}",
                input.toEntity()
        );
    }

    @Test
    public void testTreeNodeWithInternalId() {
        TreeNodeInput2 input = new TreeNodeInput2();
        input.setId(17L);
        input.setName("Root");
        assertContentEquals(
                "{\"id\":17,\"name\":\"Root\"}",
                input.toEntity()
        );
    }

    @Test
    public void testTreeNodeWithExternalId() {
        TreeNodeInput2 input = new TreeNodeInput2();
        input.setName("Root");
        assertContentEquals(
                "{\"id\":19,\"name\":\"Root\"}",
                input.toEntityById(19L)
        );
    }

    @Test
    public void testTreeNodeWithOverrideId() {
        TreeNodeInput2 input = new TreeNodeInput2();
        input.setId(17L);
        input.setName("Root");
        assertContentEquals(
                "{\"id\":19,\"name\":\"Root\"}",
                input.toEntityById(19L)
        );
    }
}
