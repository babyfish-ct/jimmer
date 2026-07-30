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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModifiedAssociationFetcherTest extends AbstractMutationTest {

    @Test
    public void testMergeOneToManyWithReturning() {
        assertDepartmentEmployees(
                AssociatedSaveMode.MERGE,
                true,
                1L,
                "Sam",
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DELETED_MILLIS from final table (" +
                                        "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) " +
                                        "tb_2_(ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set " +
                                        "NAME = tb_2_.NAME, GENDER = tb_2_.GENDER, " +
                                        "DEPARTMENT_ID = tb_2_.DEPARTMENT_ID " +
                                        "when not matched then insert(" +
                                        "ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS" +
                                        ") values(" +
                                        "tb_2_.ID, tb_2_.NAME, tb_2_.GENDER, " +
                                        "tb_2_.DEPARTMENT_ID, tb_2_.DELETED_MILLIS" +
                                        ")"
                        );
                        it.variables(1L, "Sam", "M", 1L, 0L);
                    });
                    expectEmployeeFetch(ctx);
                }
        );
    }

    @Test
    public void testMergeOneToManyWithoutReturning() {
        assertDepartmentEmployees(
                AssociatedSaveMode.MERGE,
                false,
                1L,
                "Sam",
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) " +
                                        "tb_2_(ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set " +
                                        "NAME = tb_2_.NAME, GENDER = tb_2_.GENDER, " +
                                        "DEPARTMENT_ID = tb_2_.DEPARTMENT_ID " +
                                        "when not matched then insert(" +
                                        "ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS" +
                                        ") values(" +
                                        "tb_2_.ID, tb_2_.NAME, tb_2_.GENDER, " +
                                        "tb_2_.DEPARTMENT_ID, tb_2_.DELETED_MILLIS" +
                                        ")"
                        );
                        it.variables(1L, "Sam", "M", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables(1L, 0L);
                    });
                    expectEmployeeFetch(ctx);
                }
        );
    }

    @Test
    public void testMergeOneToManyWithView() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .saveCommand(departmentWithEmployee(1L, "Sam"))
                        .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                        .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .execute(con, DepartmentCompositeView.class)
                        .getModifiedView(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DELETED_MILLIS from final table (" +
                                        "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) " +
                                        "tb_2_(ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set " +
                                        "NAME = tb_2_.NAME, GENDER = tb_2_.GENDER, " +
                                        "DEPARTMENT_ID = tb_2_.DEPARTMENT_ID " +
                                        "when not matched then insert(" +
                                        "ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS" +
                                        ") values(" +
                                        "tb_2_.ID, tb_2_.NAME, tb_2_.GENDER, " +
                                        "tb_2_.DEPARTMENT_ID, tb_2_.DELETED_MILLIS" +
                                        ")"
                        );
                        it.variables(1L, "Sam", "M", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables(1L, 0L);
                    });
                    ctx.value(view -> assertEquals(
                            new HashSet<>(Arrays.asList("Sam", "Jessica")),
                            view.getEmployees()
                                    .stream()
                                    .map(DepartmentCompositeView.TargetOf_employees::getName)
                                    .collect(Collectors.toSet())
                    ));
                }
        );
    }

    @Test
    public void testUpdateOneToMany() {
        assertDepartmentEmployees(
                AssociatedSaveMode.UPDATE,
                true,
                1L,
                "Sam",
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DELETED_MILLIS from final table (" +
                                        "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set NAME = ?, GENDER = ?, DEPARTMENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Sam", "M", 1L, 1L);
                    });
                    expectEmployeeFetch(ctx);
                }
        );
    }

    @Test
    public void testAppendOneToMany() {
        assertDepartmentEmployees(
                AssociatedSaveMode.APPEND,
                true,
                null,
                "Linda",
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DELETED_MILLIS from final table (" +
                                        "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(" +
                                        "NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID" +
                                        ") values(?, ?, ?, ?)"
                        );
                        it.variables("Linda", "M", 0L, 1L);
                    });
                    expectEmployeeFetch(ctx);
                }
        );
    }

    @Test
    public void testAppendIfAbsentOneToMany() {
        assertDepartmentEmployees(
                AssociatedSaveMode.APPEND_IF_ABSENT,
                true,
                null,
                "Linda",
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DELETED_MILLIS from final table (" +
                                        "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE tb_1_ " +
                                        "using(values(?, ?, ?, ?)) " +
                                        "tb_2_(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "on tb_1_.NAME = tb_2_.NAME and " +
                                        "tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then insert(" +
                                        "NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS" +
                                        ") values(" +
                                        "tb_2_.NAME, tb_2_.GENDER, " +
                                        "tb_2_.DEPARTMENT_ID, tb_2_.DELETED_MILLIS" +
                                        ")"
                        );
                        it.variables("Linda", "M", 1L, 0L);
                    });
                    expectEmployeeFetch(ctx);
                }
        );
    }

    @Test
    public void testReplaceOneToManyIsComplete() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
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
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, DELETED_MILLIS) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                        it.variables(1L, "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) " +
                                        "tb_2_(ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set " +
                                        "NAME = tb_2_.NAME, GENDER = tb_2_.GENDER, " +
                                        "DEPARTMENT_ID = tb_2_.DEPARTMENT_ID " +
                                        "when not matched then insert(" +
                                        "ID, NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS" +
                                        ") values(" +
                                        "tb_2_.ID, tb_2_.NAME, tb_2_.GENDER, " +
                                        "tb_2_.DEPARTMENT_ID, tb_2_.DELETED_MILLIS" +
                                        ")"
                        );
                        it.variables(1L, "Sam", "M", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE set DELETED_MILLIS = ? " +
                                        "where DEPARTMENT_ID = ? and " +
                                        "not (ID = any(?)) and DELETED_MILLIS = ?"
                        );
                        it.variables(UNKNOWN_VARIABLE, 1L, new Object[]{1L}, 0L);
                    });
                    ctx.value(department -> assertEquals(
                            Arrays.asList("Sam"),
                            department.employees()
                                    .stream()
                                    .map(it -> it.name())
                                    .collect(Collectors.toList())
                    ));
                }
        );
    }

    @Test
    public void testMergeManyToManyWithDerivedProperties() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
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
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into AUTHOR(" +
                                        "ID, FIRST_NAME, LAST_NAME, GENDER" +
                                        ") key(ID) values(?, ?, ?, ?)"
                        );
                        it.variables(
                                Constants.danId,
                                "Dan",
                                "Vanderkam",
                                "M"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "on tb_1_.BOOK_ID = tb_2_.BOOK_ID and " +
                                        "tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then insert(BOOK_ID, AUTHOR_ID) " +
                                        "values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                        it.variables(Constants.learningGraphQLId3, Constants.danId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.GENDER, " +
                                        "tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                                        "on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                        "where tb_2_.BOOK_ID = ?"
                        );
                        it.variables(Constants.learningGraphQLId3);
                    });
                    ctx.value(book -> {
                        assertEquals(
                                new HashSet<>(Arrays.asList(
                                        Constants.eveId,
                                        Constants.alexId,
                                        Constants.danId
                                )),
                                new HashSet<>(book.authorIds())
                        );
                        assertEquals(3, book.authorCount());
                        assertEquals(
                                new HashSet<>(Arrays.asList(
                                        "Eve-Procello",
                                        "Alex-Banks",
                                        "Dan-Vanderkam"
                                )),
                                new HashSet<>(book.authorFullNames())
                        );
                    });
                }
        );
    }

    @Test
    public void testBatchMergeManyToMany() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
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
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into AUTHOR(" +
                                        "ID, FIRST_NAME, LAST_NAME, GENDER" +
                                        ") key(ID) values(?, ?, ?, ?)"
                        );
                        it.variables(
                                Constants.danId,
                                "Dan",
                                "Vanderkam",
                                "M"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "on tb_1_.BOOK_ID = tb_2_.BOOK_ID and " +
                                        "tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then insert(BOOK_ID, AUTHOR_ID) " +
                                        "values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                        it.batches(2);
                        it.batchVariables(
                                0,
                                Constants.learningGraphQLId1,
                                Constants.danId
                        );
                        it.batchVariables(
                                1,
                                Constants.learningGraphQLId2,
                                Constants.danId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_2_.BOOK_ID, tb_1_.ID, " +
                                        "tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                        "from AUTHOR tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                                        "on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                        "where tb_2_.BOOK_ID = any(?)"
                        );
                        it.variables((Object) new Object[]{
                                Constants.learningGraphQLId1,
                                Constants.learningGraphQLId2
                        });
                    });
                    ctx.value(books -> {
                        Set<String> expectedNames = new HashSet<>(
                                Arrays.asList(
                                        "Eve Procello",
                                        "Alex Banks",
                                        "Dan Vanderkam"
                                )
                        );
                        for (Book book : books) {
                            assertEquals(
                                    expectedNames,
                                    book.authors()
                                            .stream()
                                            .map(it -> it.firstName() + ' ' + it.lastName())
                                            .collect(Collectors.toSet())
                            );
                        }
                    });
                }
        );
    }

    private void assertDepartmentEmployees(
            AssociatedSaveMode mode,
            boolean returningEnabled,
            Long employeeId,
            String employeeName,
            Consumer<ExpectDSLWithValue<Department>> expectSql
    ) {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
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
                        .getModifiedEntity(),
                ctx -> {
                    expectSql.accept(ctx);
                    ctx.value(department -> {
                        Set<String> expectedNames =
                                new HashSet<>(Arrays.asList("Sam", "Jessica"));
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
                    });
                }
        );
    }

    private static void expectEmployeeFetch(ExpectDSL ctx) {
        ctx.statement(it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER " +
                            "from EMPLOYEE tb_1_ " +
                            "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
            );
            it.variables(1L, 0L);
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
}
