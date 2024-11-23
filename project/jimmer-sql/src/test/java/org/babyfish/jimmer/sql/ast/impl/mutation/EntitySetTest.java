package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class EntitySetTest extends Tests {

    @Test
    public void test() {
        EntitySet<Book> books = new EntitySet<>(
                new PropId[] {
                        BookProps.ID.unwrap().getId()
                }
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.graphQLInActionId1);
                    draft.setName("GraphQL in Action");
                })
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.graphQLInActionId1);
                    draft.setPrice(new BigDecimal("49.99"));
                })
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setName("Effective type script");
                })
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setPrice(new BigDecimal("39.99"));
                })
        );
        assertContentEquals(
                "[{" +
                        "--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                        "--->\"price\":49.99" +
                        "}, {" +
                        "--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->\"price\":39.99}" +
                        "]",
                books.toString()
        );
    }

    @Test
    public void testDuplicateId() {
        EntitySet<Book> books = new EntitySet<>(
                new PropId[] {
                        BookProps.ID.unwrap().getId()
                }
        );
        books.add(
                Immutables.createBook(draft -> {
                    draft.setId(Constants.graphQLInActionId1);
                    draft.setName("GraphQL in Action");
                })
        );
        books.add(
                Immutables.createBook(draft -> {
                    draft.setId(Constants.learningGraphQLId1);
                    draft.setName("Learning GraphQL");
                })
        );
        books.add(
                Immutables.createBook(draft -> {
                    draft.setId(Constants.learningGraphQLId1);
                    draft.setName("Learning GraphQL+");
                })
        );
        books.add(
                Immutables.createBook(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setName("Effective TypeScript");
                })
        );
        books.add(
                Immutables.createBook(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setName("Effective TypeScript+");
                })
        );
        books.add(
                Immutables.createBook(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setName("Effective TypeScript++");
                })
        );
        assertContentEquals(
                "[" +
                        "--->{" +
                        "--->--->\"entity\":{" +
                        "--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                        "--->--->--->\"name\":\"GraphQL in Action\"" +
                        "--->--->}" +
                        "--->}, {" +
                        "--->--->entity:{" +
                        "--->--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                        "--->--->--->\"name\":\"Learning GraphQL+\"" +
                        "--->--->}," +
                        "--->--->originalEntities:[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                        "--->--->--->--->\"name\":\"Learning GraphQL\"" +
                        "--->--->--->}, {" +
                        "--->--->--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                        "--->--->--->--->\"name\":\"Learning GraphQL+\"" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}, {" +
                        "--->--->entity:{" +
                        "--->--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->--->--->\"name\":\"Effective TypeScript++\"" +
                        "--->--->},originalEntities:[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->--->--->--->\"name\":\"Effective TypeScript\"" +
                        "--->--->--->}, {" +
                        "--->--->--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->--->--->--->\"name\":\"Effective TypeScript+\"" +
                        "--->--->--->}, {" +
                        "--->--->--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->--->--->--->\"name\":\"Effective TypeScript++\"" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}" +
                        "]",
                books.items().toString()
        );
    }

    @Test
    public void testDuplicateKey() {
        EntitySet<Book> books = new EntitySet<>(
                new PropId[]{
                        BookProps.NAME.unwrap().getId(),
                        BookProps.EDITION.unwrap().getId()
                }
        );
        books.addAll(
                Arrays.asList(
                        Immutables.createBook(draft -> {
                            draft.setName("GraphQL in Action");
                            draft.setEdition(1);
                            draft.setPrice(new BigDecimal("46.9"));
                        }),
                        Immutables.createBook(draft -> {
                            draft.setName("GraphQL in Action");
                            draft.setEdition(2);
                            draft.setPrice(new BigDecimal("46.9"));
                        }),
                        Immutables.createBook(draft -> {
                            draft.setName("GraphQL in Action");
                            draft.setEdition(2);
                            draft.setPrice(new BigDecimal("47.9"));
                        }),
                        Immutables.createBook(draft -> {
                            draft.setName("GraphQL in Action");
                            draft.setEdition(3);
                            draft.setPrice(new BigDecimal("46.9"));
                        }),
                        Immutables.createBook(draft -> {
                            draft.setName("GraphQL in Action");
                            draft.setEdition(3);
                            draft.setPrice(new BigDecimal("47.9"));
                        }),
                        Immutables.createBook(draft -> {
                            draft.setName("GraphQL in Action");
                            draft.setEdition(3);
                            draft.setPrice(new BigDecimal("48.9"));
                        })
                )
        );
        assertContentEquals(
                "[" +
                        "--->{" +
                        "--->--->\"entity\":{" +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"edition\":1," +
                        "--->--->--->\"price\":46.9" +
                        "--->--->}" +
                        "--->}, {" +
                        "--->--->entity:{" +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"edition\":2," +
                        "--->--->--->\"price\":47.9" +
                        "--->--->},originalEntities:[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->--->\"edition\":2," +
                        "--->--->--->--->\"price\":46.9" +
                        "--->--->--->}, {" +
                        "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->--->\"edition\":2," +
                        "--->--->--->--->\"price\":47.9" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}, {" +
                        "--->--->entity:{" +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"edition\":3," +
                        "--->--->--->\"price\":48.9" +
                        "--->--->},originalEntities:[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":46.9" +
                        "--->--->--->}, {" +
                        "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":47.9" +
                        "--->--->--->}, {" +
                        "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":48.9" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}" +
                        "]",
                books.items().toString()
        );
    }
}
