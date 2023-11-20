package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentCompositeView;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentIdFunView;
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
                "{\"id\":\"3\",\"employees\":[{\"id\":\"4\"},{\"id\":\"5\"}]}",
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

    @Test
    public void testToEntityByIdFun() {
        DepartmentIdFunView view = new DepartmentIdFunView();
        view.setId("3");
        view.setEmployeeIds(Arrays.asList("4", "5"));
        assertContentEquals(
                "{\"id\":\"3\",\"employees\":[{\"id\":\"4\"},{\"id\":\"5\"}]}",
                view.toEntity()
        );
    }

    @Test
    public void testFromEntityByIdFun() {
        Department department = DepartmentDraft.$.produce(draft -> {
            draft.setId(3L);
            draft.setEmployeeIds(Arrays.asList(4L, 5L));
        });
        assertContentEquals(
                "DepartmentIdFunView(id=3, employeeIds=[4, 5])",
                new DepartmentIdFunView(department)
        );
    }

    @Test
    public void testToEntityByComposite() {

        DepartmentCompositeView view = new DepartmentCompositeView();
        view.setId("3");

        DepartmentCompositeView.TargetOf_employees employee1 = new DepartmentCompositeView.TargetOf_employees();
        employee1.setId("4");
        employee1.setName("Jim");

        DepartmentCompositeView.TargetOf_employees employee2 = new DepartmentCompositeView.TargetOf_employees();
        employee2.setId("5");
        employee2.setName("Kat");

        view.setEmployees(Arrays.asList(employee1, employee2));

        assertContentEquals(
                "{" +
                        "--->\"id\":\"3\"," +
                        "--->\"employees\":[" +
                        "--->--->{\"id\":\"4\",\"name\":\"Jim\"}," +
                        "--->--->{\"id\":\"5\",\"name\":\"Kat\"}" +
                        "--->]" +
                        "}",
                view.toEntity()
        );
    }

    @Test
    public void testFromEntityByComposite() {
        Department department = DepartmentDraft.$.produce(draft -> {
            draft.setId(3L);
            draft.addIntoEmployees(emp -> emp.setId(4L).setName("Jim"));
            draft.addIntoEmployees(emp -> emp.setId(5L).setName("Kate"));
        });
        assertContentEquals(
                "DepartmentCompositeView(" +
                        "--->id=3, " +
                        "--->employees=[" +
                        "--->--->DepartmentCompositeView.TargetOf_employees(id=4, name=Jim), " +
                        "--->--->DepartmentCompositeView.TargetOf_employees(id=5, name=Kate)" +
                        "--->]" +
                        ")",
                new DepartmentCompositeView(department)
        );
    }
}
