package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.ContactInfo;
import org.babyfish.jimmer.sql.model.embedded.ContactRecord;
import org.babyfish.jimmer.sql.model.embedded.ContactRecordTable;
import org.babyfish.jimmer.sql.model.embedded.dto.*;
import org.babyfish.jimmer.sql.model.embedded.dto.shared.ContactInfoInput;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;
import static org.junit.jupiter.api.Assertions.*;

public class EmbeddedInputTest extends AbstractQueryTest {

    static Stream<Arguments> inputs() {
        return Stream.of(
                cases(ContactRecordInput.class, false, true),
                cases(DynamicContactRecordInput.class, false, true),
                cases(FuzzyContactRecordInput.class, false, true),
                cases(ExplicitContactRecordInput.class, true, true),
                cases(DynamicExplicitContactRecordInput.class, false, true),
                cases(FuzzyExplicitContactRecordInput.class, false, false),
                cases(ReusableContactRecordInput.class, true, true),
                cases(DynamicReusableContactRecordInput.class, true, true)
        ).flatMap(it -> it);
    }

    private static Stream<Arguments> cases(Class<? extends Input<ContactRecord>> type, boolean missingLoaded, boolean nullLoaded) {
        return Stream.of(
                Arguments.of(type, "", missingLoaded, null),
                Arguments.of(type, ",\"email\":null", nullLoaded, null),
                Arguments.of(type, ",\"email\":\"new@example.com\"", true, "new@example.com")
        );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    public void testNullableFieldAndUpdate(
            Class<? extends Input<ContactRecord>> inputType,
            String emailJson,
            boolean emailLoaded,
            String email
    ) throws Exception {
        ContactRecord entity = jsonCodec().readerFor(inputType)
                .read("{\"id\":1,\"contact\":{\"name\":\"Bob\"" + emailJson + "}}")
                .toEntity();
        assertEquals(emailLoaded, ImmutableObjects.isLoaded(entity.contact(), "email"));
        if (emailLoaded) {
            assertEquals(email, entity.contact().email());
        }
        jdbc(con -> {
            clearExecutions();
            assertEquals(1, getSqlClient().saveCommand(entity).setMode(SaveMode.UPDATE_ONLY).execute(con).getTotalAffectedRowCount());
            assertEquals(1, getExecutions().size());
            Execution execution = getExecutions().get(0);
            assertEquals(emailLoaded ?
                    "update CONTACT_RECORD set NAME = ?, EMAIL = ? where ID = ?" :
                    "update CONTACT_RECORD set NAME = ? where ID = ?", execution.getSql());
            assertEquals(1, execution.getBatchCount());
            Object emailVariable = email != null ? email : new DbLiteral.DbNull(String.class);
            assertEquals(emailLoaded ? Arrays.asList("Bob", emailVariable, 1L) : Arrays.asList("Bob", 1L), execution.getVariables(0));
            try (Statement statement = con.createStatement();
                 ResultSet rs = statement.executeQuery("select NAME, EMAIL from CONTACT_RECORD where ID = 1")) {
                assertTrue(rs.next());
                assertEquals("Bob", rs.getString(1));
                assertEquals(emailLoaded ? email : "old@example.com", rs.getString(2));
                assertFalse(rs.next());
            }
        });
    }

    @Test
    public void testGeneratedPropertyTypes() throws Exception {
        assertEquals(ContactInfo.class, ContactRecordInput.class.getMethod("getContact").getReturnType());
        assertEquals(ExplicitContactRecordInput.TargetOf_contact.class,
                ExplicitContactRecordInput.class.getMethod("getContact").getReturnType());
        assertEquals(ContactInfoInput.class, ReusableContactRecordInput.class.getMethod("getContact").getReturnType());
        assertEquals(ContactInfoInput.class, DynamicReusableContactRecordInput.class.getMethod("getContact").getReturnType());
    }

    @Test
    public void testReusableEmbeddedView() {
        ContactRecordTable table = ContactRecordTable.$;
        executeAndExpect(getSqlClient().createQuery(table).where(table.id().eq(1L))
                .select(table.fetch(ReusableContactRecordView.class)), ctx -> {
            ctx.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.EMAIL from CONTACT_RECORD tb_1_ where tb_1_.ID = ?");
            ctx.variables(1L);
            ctx.rows(rows -> {
                assertEquals(1, rows.size());
                ContactRecord entity = rows.get(0).toEntity();
                assertEquals("Alice", entity.contact().name());
                assertEquals("old@example.com", entity.contact().email());
            });
        });
    }

    @Test
    public void testFixedNestedInputRequiresNullableField() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonCodec().readerFor(FixedExplicitContactRecordInput.class)
                .read("{\"id\":1,\"contact\":{\"name\":\"Alice\"}}"));
        assertTrue(ex.getMessage().contains("email"));
        assertTrue(ex.getMessage().contains("not specified by JSON explicitly"));
    }

    @Test
    public void testFixedNestedInputAcceptsExplicitNull() throws Exception {
        ContactRecord entity = jsonCodec().readerFor(FixedExplicitContactRecordInput.class)
                .read("{\"id\":1,\"contact\":{\"name\":\"Alice\",\"email\":null}}")
                .toEntity();
        assertTrue(ImmutableObjects.isLoaded(entity.contact(), "email"));
        assertNull(entity.contact().email());
    }
}
