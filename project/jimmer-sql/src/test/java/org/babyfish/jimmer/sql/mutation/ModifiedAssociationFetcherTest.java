package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentFetcher;
import org.babyfish.jimmer.sql.model.hr.EmployeeFetcher;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentCompositeView;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModifiedAssociationFetcherTest extends AbstractMutationTest {

    @Test
    public void testMergeOneToManyWithReturning() {
        assertDepartmentEmployees(AssociatedSaveMode.MERGE, true, 1L, "Sam");
    }

    @Test
    public void testMergeOneToManyWithoutReturning() {
        assertDepartmentEmployees(AssociatedSaveMode.MERGE, false, 1L, "Sam");
    }

    @Test
    public void testMergeOneToManyWithView() {
        jdbc(con -> {
            clearExecutions();
            DepartmentCompositeView view = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(departmentWithEmployee(1L, "Sam"))
                    .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                    .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                    .execute(con, DepartmentCompositeView.class)
                    .getModifiedView();
            assertEquals(
                    new HashSet<>(Arrays.asList("Sam", "Jessica")),
                    view.getEmployees()
                            .stream()
                            .map(DepartmentCompositeView.TargetOf_employees::getName)
                            .collect(Collectors.toSet())
            );
            assertTrue(fetcherExecutionCount() > 0L);
        });
    }

    @Test
    public void testUpdateOneToMany() {
        assertDepartmentEmployees(AssociatedSaveMode.UPDATE, true, 1L, "Sam");
    }

    @Test
    public void testAppendOneToMany() {
        assertDepartmentEmployees(AssociatedSaveMode.APPEND, true, null, "Linda");
    }

    @Test
    public void testAppendIfAbsentOneToMany() {
        assertDepartmentEmployees(AssociatedSaveMode.APPEND_IF_ABSENT, true, null, "Linda");
    }

    @Test
    public void testReplaceOneToManyIsComplete() {
        jdbc(con -> {
            clearExecutions();
            Department department = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(
                            Immutables.createDepartment(draft -> {
                                draft.setId(1L);
                                draft.setName("Market");
                                draft.addIntoEmployees(employee -> {
                                    employee.setId(1L);
                                    employee.setName("Sam");
                                    employee.setGender(Gender.MALE);
                                });
                            })
                    )
                    .setAssociatedModeAll(AssociatedSaveMode.REPLACE)
                    .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                    .execute(
                            con,
                            DepartmentFetcher.$
                                    .name()
                                    .employees(EmployeeFetcher.$.allScalarFields())
                    )
                    .getModifiedEntity();
            List<String> employeeNames = department.employees()
                    .stream()
                    .map(it -> it.name())
                    .collect(Collectors.toList());
            assertEquals(Arrays.asList("Sam"), employeeNames);
            assertEquals(0L, fetcherExecutionCount());
        });
    }

    @Test
    public void testMergeManyToManyWithDerivedProperties() {
        jdbc(con -> {
            clearExecutions();
            Book book = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(bookWithAuthor(Constants.learningGraphQLId3))
                    .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                    .execute(
                            con,
                            BookFetcher.$
                                    .authors(AuthorFetcher.$.allScalarFields())
                                    .authorIds()
                                    .authorCount()
                                    .authorFullNames()
                    )
                    .getModifiedEntity();
            assertEquals(
                    new HashSet<>(Arrays.asList(Constants.eveId, Constants.alexId, Constants.danId)),
                    new HashSet<>(book.authorIds())
            );
            assertEquals(3, book.authorCount());
            assertEquals(
                    new HashSet<>(Arrays.asList("Eve-Procello", "Alex-Banks", "Dan-Vanderkam")),
                    new HashSet<>(book.authorFullNames())
            );
            assertEquals(1L, fetcherExecutionCount());
        });
    }

    @Test
    public void testBatchMergeManyToMany() {
        jdbc(con -> {
            clearExecutions();
            List<Book> books = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveEntitiesCommand(
                            Arrays.asList(
                                    bookWithAuthor(Constants.learningGraphQLId1),
                                    bookWithAuthor(Constants.learningGraphQLId2)
                            )
                    )
                    .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                    .execute(
                            con,
                            BookFetcher.$.authors(AuthorFetcher.$.allScalarFields())
                    )
                    .getItems()
                    .stream()
                    .map(BatchSaveResult.Item::getModifiedEntity)
                    .collect(Collectors.toList());
            Set<String> expectedNames =
                    new HashSet<>(Arrays.asList("Eve Procello", "Alex Banks", "Dan Vanderkam"));
            for (Book book : books) {
                assertEquals(
                        expectedNames,
                        book.authors()
                                .stream()
                                .map(it -> it.firstName() + ' ' + it.lastName())
                                .collect(Collectors.toSet())
                );
            }
            assertEquals(1L, fetcherExecutionCount());
        });
    }

    private void assertDepartmentEmployees(
            AssociatedSaveMode mode,
            boolean returningEnabled,
            Long employeeId,
            String employeeName
    ) {
        jdbc(con -> {
            clearExecutions();
            Department department = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(departmentWithEmployee(employeeId, employeeName))
                    .setAssociatedModeAll(mode)
                    .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                    .setSaveReturningEnabled(returningEnabled)
                    .execute(
                            con,
                            DepartmentFetcher.$
                                    .name()
                                    .employees(EmployeeFetcher.$.allScalarFields())
                    )
                    .getModifiedEntity();
            Set<String> expectedNames = new HashSet<>(Arrays.asList("Sam", "Jessica"));
            if (employeeId == null) {
                expectedNames.add("Linda");
            }
            assertEquals(
                    expectedNames,
                    department.employees()
                            .stream()
                            .map(it -> it.name())
                            .collect(Collectors.toSet())
            );
            assertTrue(fetcherExecutionCount() > 0L);
        });
    }

    private static Department departmentWithEmployee(Long employeeId, String employeeName) {
        return Immutables.createDepartment(draft -> {
            draft.setId(1L);
            draft.setName("Market");
            draft.addIntoEmployees(employee -> {
                if (employeeId != null) {
                    employee.setId(employeeId);
                }
                employee.setName(employeeName);
                employee.setGender(Gender.MALE);
            });
        });
    }

    private static Book bookWithAuthor(UUID bookId) {
        return Immutables.createBook(draft -> {
            draft.setId(bookId);
            draft.addIntoAuthors(author -> {
                author.setId(Constants.danId);
                author.setFirstName("Dan");
                author.setLastName("Vanderkam");
                author.setGender(Gender.MALE);
            });
        });
    }

    private long fetcherExecutionCount() {
        return getExecutions()
                .stream()
                .filter(it -> {
                    ExecutionPurpose purpose = it.getPurpose();
                    return purpose instanceof ExecutionPurpose.Command &&
                            ((ExecutionPurpose.Command) purpose).getQueryReason() == QueryReason.FETCHER;
                })
                .count();
    }
}
