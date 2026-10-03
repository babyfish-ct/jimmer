package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.hr.*;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse.Citizen;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse.CitizenDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BackReferenceSaveTest extends AbstractMutationTest {

    @ParameterizedTest
    @EnumSource(value = AssociatedSaveMode.class, names = {"REPLACE", "MERGE", "APPEND", "APPEND_IF_ABSENT", "UPDATE"})
    void referencesOnlyUpdateForeignKey(AssociatedSaveMode mode) {
        assertReferences(getSqlClient(it -> {}), null, mode, true);
    }

    @ParameterizedTest
    @CsvSource({"REPLACE,false", "REPLACE,true", "MERGE,true"})
    void referencesOnMySql(AssociatedSaveMode mode, boolean batch) {
        NativeDatabases.assumeNativeDatabase();
        jdbc(NativeDatabases.MYSQL_DATA_SOURCE, false, con -> initDatabase(con, "database-mysql.sql"));
        assertReferences(getSqlClient(it -> it.setDialect(new MySqlDialect()).setExplicitBatchEnabled(batch).setDumbBatchAcceptable(batch)),
                NativeDatabases.MYSQL_DATA_SOURCE, mode, batch && mode != AssociatedSaveMode.REPLACE);
    }

    @Test
    void mixedReferencesAndEntitiesKeepCallbacks() {
        AtomicInteger processed = new AtomicInteger();
        AtomicInteger intercepted = new AtomicInteger();
        JSqlClient client = getSqlClient(it -> it
                .addDraftPreProcessor(new DraftPreProcessor<EmployeeDraft>() {
                    @Override
                    public void beforeSave(EmployeeDraft draft) {
                        processed.incrementAndGet();
                        assertEquals("New employee", draft.name());
                    }
                })
                .addDraftInterceptor(new DraftInterceptor<Employee, EmployeeDraft>() {
                    @Override
                    public void beforeSave(EmployeeDraft draft, Employee original) {
                        intercepted.incrementAndGet();
                        assertEquals("New employee", draft.name());
                        assertNull(original);
                    }
                }));
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("Mixed")
                .addIntoEmployees(e -> e.setId(1))
                .addIntoEmployees(e -> e.setName("New employee").setGender(Gender.MALE)));
        jdbc(con -> {
            SimpleSaveResult<Department> result = client.saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED).execute(con);
            assertEquals(3, result.getTotalAffectedRowCount());
            assertEquals(1, processed.get());
            assertEquals(1, intercepted.get());
            // Parent insert, reference update, interceptor lookup of the new entity, entity insert.
            assertEquals(4, getExecutions().size());
            assertEquals("update EMPLOYEE set DEPARTMENT_ID = ? where ID = ?", getExecutions().get(1).getSql());
            assertEquals(1, getExecutions().get(1).getBatchCount());
            Department saved = result.getModifiedEntity();
            assertEquals(1, saved.employees().get(0).id());
            assertNotEquals(1, saved.employees().get(1).id());
            for (Employee e : saved.employees()) {
                assertEquals(saved.id(), e.department().id());
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitEntityIntentIsPreserved(boolean explicitBackReference) {
        AtomicInteger processed = new AtomicInteger();
        JSqlClient client = getSqlClient(it -> it.addDraftPreProcessor(new DraftPreProcessor<EmployeeDraft>() {
            @Override
            public void beforeSave(EmployeeDraft draft) {
                processed.incrementAndGet();
                draft.setName("Explicit entity").setGender(Gender.FEMALE);
            }
        }));
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("Entity parent").addIntoEmployees(e -> {
            e.setId(900);
            if (explicitBackReference) {
                e.setDepartmentId(1L);
            }
        }));
        jdbc(con -> {
            SimpleSaveResult<Department> result = client.saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                    .setIdOnlyAsReference(DepartmentProps.EMPLOYEES, explicitBackReference)
                    .setAssociatedMode(DepartmentProps.EMPLOYEES, AssociatedSaveMode.APPEND)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED).execute(con);
            assertEquals(2, result.getTotalAffectedRowCount());
            assertEquals(1, processed.get());
            assertEquals(2, getExecutions().size());
            assertTrue(getExecutions().get(1).getSql().startsWith("insert into EMPLOYEE"));
            Employee stored = client.getEntities().forConnection(con).findById(Employee.class, 900L);
            assertNotNull(stored);
            assertEquals("Explicit entity", stored.name());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingReferenceNeverInserts(boolean checked) {
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("Missing target")
                .setEmployeeIds(Collections.singletonList(999L)));
        jdbc(con -> {
            SimpleEntitySaveCommand<Department> command = getSqlClient(it -> {}).saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                    .setAutoIdOnlyTargetChecking(DepartmentProps.EMPLOYEES, checked)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED);
            if (checked) {
                assertThrows(SaveException.IllegalTargetId.class, () -> command.execute(con));
                assertEquals(2, getExecutions().size());
                assertTrue(getExecutions().get(1).getSql().startsWith("select "));
            } else {
                assertEquals(1, command.execute(con).getTotalAffectedRowCount());
                assertEquals(2, getExecutions().size());
                assertEquals("update EMPLOYEE set DEPARTMENT_ID = ? where ID = ?", getExecutions().get(1).getSql());
            }
        });
    }

    @Test
    void transferStillRequiresPermission() {
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("Blocked")
                .setEmployeeIds(Collections.singletonList(1L)));
        jdbc(con -> {
            assertThrows(SaveException.TargetIsNotTransferable.class, () -> getSqlClient(it -> {}).saveCommand(input)
                    .setMode(SaveMode.INSERT_ONLY)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.NOT_ALLOWED).execute(con));
            assertEquals(2, getExecutions().size());
            assertTrue(getExecutions().get(1).getSql().startsWith("select "));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nullableForeignKeyKeepsTransferPolicy(boolean transferable) {
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("First parent")
                .setEmployeeIds(Collections.singletonList(4L)));
        jdbc(con -> {
            try (java.sql.Statement stmt = con.createStatement()) {
                stmt.executeUpdate("insert into EMPLOYEE(ID, NAME, GENDER) values(4, 'Unassigned', 'M')");
            }
            SimpleEntitySaveCommand<Department> command = getSqlClient(it -> {}).saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES,
                            transferable ? TargetTransferMode.ALLOWED : TargetTransferMode.NOT_ALLOWED);
            if (!transferable) {
                // The existing transfer rule protects a null FK as well as a different non-null parent.
                assertThrows(SaveException.TargetIsNotTransferable.class, () -> command.execute(con));
                assertEquals(2, getExecutions().size());
                return;
            }
            SimpleSaveResult<Department> result = command.execute(con);
            assertEquals(2, result.getTotalAffectedRowCount());
            assertEquals(2, getExecutions().size());
            assertEquals("update EMPLOYEE set DEPARTMENT_ID = ? where ID = ?", getExecutions().get(1).getSql());
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    void eventsAndReturningDescribeForeignKeyUpdate() {
        JSqlClient client = getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY));
        List<EntityEvent<Employee>> events = new ArrayList<>();
        List<AssociationEvent> links = new ArrayList<>();
        client.getTriggers(true).addEntityListener(Employee.class, events::add);
        client.getTriggers(true).addAssociationListener(DepartmentProps.EMPLOYEES, links::add);
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("Events")
                .setEmployeeIds(Collections.singletonList(1L)));
        jdbc(con -> {
            SimpleSaveResult<Department> result = client.saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED)
                    .execute(con, DepartmentFetcher.$.employees(EmployeeFetcher.$.name().department()));
            assertEquals(2, result.getTotalAffectedRowCount());
            Department saved = result.getModifiedEntity();
            assertEquals("Sam", saved.employees().get(0).name());
            assertEquals(saved.id(), saved.employees().get(0).department().id());
            assertEquals(1, events.size());
            assertEquals(1, events.get(0).getOldEntity().department().id());
            assertEquals(saved.id(), events.get(0).getNewEntity().department().id());
            assertEquals(3, links.size(), links::toString);
            assertEquals(1, links.stream().filter(AssociationEvent::isEvict).count());
            assertTrue(links.stream().anyMatch(e -> !e.isEvict() && Long.valueOf(1).equals(e.getDetachedTargetId())));
            assertTrue(links.stream().anyMatch(e -> !e.isEvict() && Long.valueOf(1).equals(e.getAttachedTargetId())));
            assertEquals(1, getExecutions().stream().filter(e -> e.getSql().startsWith("update EMPLOYEE")).count());
            assertEquals(0, getExecutions().stream().filter(e -> e.getSql().contains("into EMPLOYEE")).count());
        });
    }

    @Test
    void replacementStillDetachesOmittedTargets() {
        Department input = DepartmentDraft.$.produce(draft -> draft.setId(1).setEmployeeIds(Collections.singletonList(1L)));
        jdbc(con -> {
            JSqlClient client = getSqlClient(it -> {});
            client.saveCommand(input).setMode(SaveMode.UPDATE_ONLY)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED).execute(con);
            Department stored = client.getEntities().forConnection(con).findById(DepartmentFetcher.$.employees(), 1L);
            assertEquals(Collections.singletonList(1L), stored.employeeIds());
            assertEquals(0, getExecutions().stream().filter(e -> e.getSql().contains("into EMPLOYEE")).count());
        });
    }

    @Test
    void explicitAssignmentRetainsEntityProcessing() {
        AtomicInteger processed = new AtomicInteger();
        JSqlClient client = getSqlClient(it -> it.addDraftPreProcessor(new DraftPreProcessor<EmployeeDraft>() {
            @Override
            public void beforeSave(EmployeeDraft draft) {
                processed.incrementAndGet();
                draft.setName("Assignment target");
            }
        }));
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("Assignment")
                .setEmployeeIds(Collections.singletonList(1L)));
        jdbc(con -> {
            SimpleSaveResult<Department> result = client.saveCommand(input)
                    .setAssociatedMode(DepartmentProps.EMPLOYEES, AssociatedSaveMode.UPDATE)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED)
                    .set(EmployeeTable.class, EmployeeProps.NAME, (table, values) -> table.name().concat("-updated"))
                    .execute(con);
            assertEquals(2, result.getTotalAffectedRowCount());
            assertEquals(1, processed.get());
            assertEquals("Sam-updated", client.getEntities().forConnection(con).findById(Employee.class, 1L).name());
        });
    }

    @Test
    void inverseOneToOneUpdatesJoinedSubtypeForeignKey() {
        Citizen input = CitizenDraft.$.produce(draft -> draft.setName("New holder").applyPassport(p -> p.setId(701)));
        jdbc(con -> {
            SimpleSaveResult<Citizen> result = getSqlClient(it -> {}).saveCommand(input).setMode(SaveMode.INSERT_ONLY).execute(con);
            assertEquals(701, result.getModifiedEntity().passport().id());
            assertEquals(result.getModifiedEntity().id(), result.getModifiedEntity().passport().citizen().id());
            assertEquals(0, getExecutions().stream().filter(e -> e.getSql().startsWith("insert into JOINED_DOCUMENT") ||
                    e.getSql().startsWith("insert into JOINED_PASSPORT")).count());
            try (java.sql.Statement stmt = con.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("select d.NAME, p.CITIZEN_ID from JOINED_DOCUMENT d " +
                         "inner join JOINED_PASSPORT p on d.ID = p.ID where p.ID = 701")) {
                assertTrue(rs.next());
                assertEquals("primary-passport", rs.getString(1));
                assertEquals(result.getModifiedEntity().id(), rs.getLong(2));
            }
        });
    }

    private void assertReferences(JSqlClient client, DataSource dataSource, AssociatedSaveMode mode, boolean batch) {
        Department input = DepartmentDraft.$.produce(draft -> draft.setName("New department").setEmployeeIds(Arrays.asList(1L, 2L)));
        jdbc(dataSource, true, con -> {
            SimpleSaveResult<Department> result = client.saveCommand(input)
                    .setMode(SaveMode.INSERT_ONLY)
                    .setAssociatedMode(DepartmentProps.EMPLOYEES, mode)
                    .setTargetTransferMode(DepartmentProps.EMPLOYEES, TargetTransferMode.ALLOWED).execute(con);
            assertEquals(3, result.getTotalAffectedRowCount());
            assertEquals(batch ? 2 : 3, getExecutions().size(), () -> getExecutions().stream()
                    .map(e -> e.getSql() + " [batch=" + e.getBatchCount() + "]").collect(java.util.stream.Collectors.joining("\n")));
            Execution update = getExecutions().get(1);
            assertEquals("update EMPLOYEE set DEPARTMENT_ID = ? where ID = ?", update.getSql());
            assertEquals(batch ? 2 : 1, update.getBatchCount());
            long departmentId = result.getModifiedEntity().id();
            assertEquals(Arrays.asList(departmentId, 1L), update.getVariables(0));
            if (batch) {
                assertEquals(Arrays.asList(departmentId, 2L), update.getVariables(1));
            } else {
                assertEquals(update.getSql(), getExecutions().get(2).getSql());
                assertEquals(Arrays.asList(departmentId, 2L), getExecutions().get(2).getVariables(0));
            }
            for (Employee employee : result.getModifiedEntity().employees()) {
                assertEquals(departmentId, employee.department().id());
            }
            try (java.sql.Statement stmt = con.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("select NAME, DEPARTMENT_ID from EMPLOYEE where ID in (1, 2) order by ID")) {
                assertTrue(rs.next());
                assertEquals("Sam", rs.getString(1));
                assertEquals(departmentId, rs.getLong(2));
                assertTrue(rs.next());
                assertEquals("Jessica", rs.getString(1));
                assertEquals(departmentId, rs.getLong(2));
                assertFalse(rs.next());
            }
        });
    }
}
