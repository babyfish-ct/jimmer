package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.ld.validate.E;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;

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
        Iterator<EntityCollection.Item<Book>> itr = books.items().iterator();
        itr.next();
        itr.next();
        itr.next();
        itr.remove();
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
                        "--->}" +
                        "]",
                books.items().toString()
        );
        itr = books.items().iterator();
        itr.next();
        itr.remove();
        assertContentEquals(
                "[" +
                        "--->{" +
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
                        "--->}" +
                        "]",
                books.items().toString()
        );
        itr = books.items().iterator();
        itr.next();
        itr.remove();
        assertContentEquals(
                "[]",
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

    @Test
    public void testEsNode() {
        EsNode<Book> books = new EsNode<>(
                0,
                Immutables.createBook(draft -> {}),
                null,
                null,
                null
        );
        assertContentEquals("[{}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}, {}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}, {}, {}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}, {}, {}, {}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}, {}, {}, {}, {}, {}]", books.getOriginalEntities());
        books.merge(Immutables.createBook(draft -> {}));
        assertContentEquals("[{}, {}, {}, {}, {}, {}, {}, {}, {}]", books.getOriginalEntities());
    }

    @Test
    public void testGrow() throws NoSuchFieldException, IllegalAccessException {
        EntitySet<TreeNode> set = new EntitySet<>(
                new PropId[] { TreeNodeProps.ID.unwrap().getId() }
        );
        int d1 = 200, d2 = 10;
        for (int i = 0; i < d1; i++) {
            for (int ii = 0; ii < d2; ii++) {
                if (i % 2 == 0 && ii % 2 == 0) {
                    long id = i;
                    String name = "name-" + ii;
                    TreeNode treeNode = Immutables.createTreeNode(draft -> {
                        draft.setId(id);
                        draft.setName(name);
                    });
                    set.add(treeNode);
                }
            }
        }
        for (int i = 0; i < d1; i++) {
            for (int ii = 0; ii < d2; ii++) {
                long id = i;
                String name = "name-" + ii;
                TreeNode treeNode = Immutables.createTreeNode(draft -> {
                    draft.setId(id);
                    draft.setName(name);
                });
                Assertions.assertEquals(
                        i % 2 == 0,
                        set.contains(treeNode),
                        String.format("i = %d, ii = %d", i, ii)
                );
            }
        }
        int count1 = 0, count2 = 0;
        for (TreeNode treeNode : set) {
            count1++;
        }
        for (EntityCollection.Item<TreeNode> item : set.items()) {
            count2++;
            Assertions.assertEquals(0L, item.getEntity().id() % 2);
            int originalCount = 0;
            for (TreeNode treeNode : item.getOriginalEntities()) {
                originalCount++;
            }
            Assertions.assertEquals(d2 / 2, originalCount);
        }
        Assertions.assertEquals(d1 / 2, set.size());
        Assertions.assertEquals(d1 / 2, count1);
        Assertions.assertEquals(d1 / 2, count2);
        Field field = EntitySet.class.getDeclaredField("tab");
        field.setAccessible(true);
        EsNode<?>[] nodes = (EsNode<?>[])field.get(set);
        Assertions.assertEquals(8 << 6, nodes.length);
    }
}
