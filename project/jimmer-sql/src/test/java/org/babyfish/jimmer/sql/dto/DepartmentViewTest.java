package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class DepartmentViewTest extends Tests {

    @Test
    public void testToEntity() {
        DepartmentView view = new DepartmentView();
        view.setId("3");
        view.setEmployeeIds(Arrays.asList("4", "5"));
        assertContentEquals(
                "{\"id\":\"3\",\"employees\":[{\"id\":4},{\"id\":5}]}",
                view.toEntity()
        );
    }

    @Test
    public void testFromEntity() {
        Department department = DepartmentDraft.$.produce(draft -> {
            draft.setId(3L);
            draft.setEmployeeIds(Arrays.asList(4L, 5L));
        });
        assertContentEquals(
                "DepartmentView(id=3, employeeIds=[4, 5])",
                new DepartmentView(department)
        );
    }
}
