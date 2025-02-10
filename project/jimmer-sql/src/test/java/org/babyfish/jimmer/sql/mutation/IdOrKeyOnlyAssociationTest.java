package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

public class IdOrKeyOnlyAssociationTest extends AbstractMutationTest {

    @Test
    public void testIdOnlyAsReference() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.setAuthorIds(Arrays.asList(Constants.alexId, Constants.danId));
        });
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(book),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testIdOnlyAsEntity() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.setAuthorIds(Arrays.asList(Constants.alexId, Constants.danId));
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addDraftPreProcessor(new DraftPreProcessor<AuthorDraft>() {
                        @Override
                        public void beforeSave(@NotNull AuthorDraft draft) {
                            if (draft.id().equals(Constants.alexId)) {
                                draft.setFirstName("Alex");
                                draft.setLastName("Banks");
                                draft.setGender(Gender.MALE);
                            } else if (draft.id().equals(Constants.danId)) {
                                draft.setFirstName("Dan");
                                draft.setLastName("Vanderkam");
                                draft.setGender(Gender.MALE);
                            }
                        }
                    });
                }).getEntities()
                        .saveCommand(book)
                        .setIdOnlyAsReference(BookProps.AUTHORS, false),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) " +
                                        "key(ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testKeyOnlyAsReference() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.addIntoAuthors(author -> {
                author.setFirstName("Alex");
                author.setLastName("Banks");
            });
            draft.addIntoAuthors(author -> {
                author.setFirstName("Dan");
                author.setLastName("Vanderkam");
            });
        });
        executeAndExpectResult(
                getSqlClient().getEntities()
                        .saveCommand(book)
                        .setKeyOnlyAsReferenceAll(),
                        // or `setKeyOnlyAsReference(BookProps.AUTHORS)`,
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.KEY_ONLY_AS_REFERENCE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testKeyOnlyAsEntity() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.addIntoAuthors(author -> {
                author.setFirstName("Alex");
                author.setLastName("Banks");
            });
            draft.addIntoAuthors(author -> {
                author.setFirstName("Dan");
                author.setLastName("Vanderkam");
            });
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addDraftPreProcessor(new DraftPreProcessor<AuthorDraft>() {
                        @Override
                        public void beforeSave(@NotNull AuthorDraft draft) {
                            draft.setGender(Gender.MALE);
                        }
                    });
                }).getEntities()
                        .saveCommand(book),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("update AUTHOR set GENDER = ? where ID = ?");
                        it.batches(2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }
}
