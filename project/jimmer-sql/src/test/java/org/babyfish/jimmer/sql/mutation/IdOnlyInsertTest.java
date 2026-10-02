package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.babyfish.jimmer.sql.model.idonly.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IdOnlyInsertTest extends AbstractMutationTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testInsertWithBackwardAssociations(boolean nested) {
        Document document = DocumentDraft.$.produce(draft -> {
            draft.addIntoFiles(file -> file.setLink("first"));
            draft.addIntoFiles(file -> file.setLink("second"));
        });
        JSqlClient client = getSqlClient(it -> it.setIdGenerator(new UUIDIdGenerator()));
        jdbc(con -> {
            Document saved;
            if (nested) {
                File input = FileDraft.$.produce(draft -> draft.setDocument(document).setLink("outer"));
                SimpleSaveResult<File> result = client.saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND).execute(con);
                assertTrue(result.isAccepted());
                assertEquals(4, result.getTotalAffectedRowCount());
                saved = result.getModifiedEntity().document();
            } else {
                SimpleSaveResult<Document> result = client.saveCommand(document).setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND).execute(con);
                assertTrue(result.isAccepted());
                assertEquals(3, result.getTotalAffectedRowCount());
                saved = result.getModifiedEntity();
            }
            assertEquals(nested ? 3 : 2, getExecutions().size());
            assertEquals("insert into ID_ONLY_DOCUMENT(ID) values(?)", getExecutions().get(0).getSql());
            assertEquals(Collections.singletonList(saved.id()), getExecutions().get(0).getVariables(0));
            assertEquals("insert into ID_ONLY_FILE(ID, DOCUMENT_ID, LINK) values(?, ?, ?)", getExecutions().get(1).getSql());
            assertEquals(2, getExecutions().get(1).getBatchCount());
            for (int index = 0; index < 2; index++) {
                File file = saved.files().get(index);
                assertEquals(saved.id(), file.document().id());
                assertEquals(Arrays.asList(file.id(), saved.id(), file.link()), getExecutions().get(1).getVariables(index));
            }
            Document stored = client.getEntities().forConnection(con)
                    .findById(DocumentFetcher.$.name().files(FileFetcher.$.link()), saved.id());
            assertNotNull(stored);
            assertNull(stored.name());
            assertEquals(nested ? 3 : 2, stored.files().size());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testIdOnlyRoot(boolean asReference) {
        UUID id = UUID.randomUUID();
        Document document = DocumentDraft.$.produce(draft -> draft.setId(id));
        JSqlClient client = getSqlClient(it -> it.setIdGenerator(new UUIDIdGenerator()));
        jdbc(con -> {
            SimpleSaveResult<Document> result = client.saveCommand(document).setMode(SaveMode.INSERT_ONLY)
                    .setIdOnlyAsReferenceAll(asReference).execute(con);
            assertTrue(result.isAccepted());
            assertEquals(asReference ? 0 : 1, result.getTotalAffectedRowCount());
            assertEquals(asReference ? 0 : 1, getExecutions().size());
            if (!asReference) {
                assertEquals("insert into ID_ONLY_DOCUMENT(ID) values(?)", getExecutions().get(0).getSql());
                assertEquals(Collections.singletonList(id), getExecutions().get(0).getVariables(0));
            }
            assertEquals(!asReference, client.getEntities().forConnection(con).findById(Document.class, id) != null);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testIdOnlyAssociatedObject(boolean asReference) {
        UUID id = UUID.randomUUID();
        JSqlClient client = getSqlClient(it -> it.setIdGenerator(new UUIDIdGenerator()));
        jdbc(con -> {
            if (asReference) {
                try (java.sql.PreparedStatement stmt = con.prepareStatement("insert into ID_ONLY_DOCUMENT(ID) values(?)")) {
                    stmt.setObject(1, id);
                    stmt.executeUpdate();
                }
            }
            File input = FileDraft.$.produce(draft -> draft.applyDocument(doc -> doc.setId(id)).setLink("file"));
            SimpleSaveResult<File> result = client.saveCommand(input).setMode(SaveMode.INSERT_ONLY)
                    .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                    .setIdOnlyAsReference(FileProps.DOCUMENT, asReference).execute(con);
            assertTrue(result.isAccepted());
            assertEquals(asReference ? 1 : 2, result.getTotalAffectedRowCount());
            assertEquals(asReference ? 1 : 2, getExecutions().size());
            if (!asReference) {
                assertEquals("insert into ID_ONLY_DOCUMENT(ID) values(?)", getExecutions().get(0).getSql());
            }
            assertEquals("insert into ID_ONLY_FILE(ID, DOCUMENT_ID, LINK) values(?, ?, ?)",
                    getExecutions().get(asReference ? 0 : 1).getSql());
            assertNotNull(client.getEntities().forConnection(con).findById(Document.class, id));
            assertNotNull(client.getEntities().forConnection(con).findById(File.class, result.getModifiedEntity().id()));
        });
    }

    @Test
    public void testIdOnlyInsertWithReturningAndTrigger() {
        UUID id = UUID.randomUUID();
        Document document = DocumentDraft.$.produce(draft -> draft.setId(id));
        JSqlClient client = getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY));
        List<EntityEvent<Document>> events = new ArrayList<>();
        client.getTriggers().addEntityListener(Document.class, events::add);
        jdbc(con -> {
            SimpleSaveResult<Document> result = client.saveCommand(document).setMode(SaveMode.INSERT_ONLY)
                    .setIdOnlyAsReferenceAll(false).execute(con, DocumentFetcher.$.name());
            assertTrue(result.isAccepted());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals(id, result.getModifiedEntity().id());
            assertNull(result.getModifiedEntity().name());
            assertEquals(1, getExecutions().size());
            assertEquals("insert into ID_ONLY_DOCUMENT(ID) values(?)", getExecutions().get(0).getSql());
            assertEquals(Collections.singletonList(id), getExecutions().get(0).getVariables(0));
            assertEquals(1, events.size());
            assertNull(events.get(0).getOldEntity());
            assertEquals(id, events.get(0).getNewEntity().id());
        });
    }

    @Test
    public void testIdOnlyEntityStillRequiresMandatoryProperties() {
        File file = FileDraft.$.produce(draft -> draft.setId(UUID.randomUUID()));
        jdbc(con -> {
            ExecutionException ex = assertThrows(ExecutionException.class, () ->
                    getSqlClient().saveCommand(file).setMode(SaveMode.INSERT_ONLY).setIdOnlyAsReferenceAll(false).execute(con));
            assertTrue(ex.getCause() instanceof java.sql.SQLException);
            assertEquals("23502", ((java.sql.SQLException) ex.getCause()).getSQLState());
            // Constraint violations use the existing exception-investigation query.
            assertEquals(2, getExecutions().size());
            assertEquals("insert into ID_ONLY_FILE(ID) values(?)", getExecutions().get(0).getSql());
            assertEquals("select tb_1_.ID from ID_ONLY_FILE tb_1_ where tb_1_.ID = ?", getExecutions().get(1).getSql());
            assertEquals(Collections.singletonList(file.id()), getExecutions().get(1).getVariables(0));
        });
    }

    @Test
    public void testIdOnlyBatchAsEntities() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        executeAndExpectResult(
                getSqlClient().saveEntitiesCommand(Arrays.asList(
                        DocumentDraft.$.produce(draft -> draft.setId(firstId)),
                        DocumentDraft.$.produce(draft -> draft.setId(secondId))
                )).setMode(SaveMode.INSERT_ONLY).setIdOnlyAsReferenceAll(false),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into ID_ONLY_DOCUMENT(ID) values(?)");
                        it.batchVariables(0, firstId);
                        it.batchVariables(1, secondId);
                    });
                    ctx.totalRowCount(2);
                    ctx.entity(it -> {});
                    ctx.entity(it -> {});
                }
        );
    }
}
