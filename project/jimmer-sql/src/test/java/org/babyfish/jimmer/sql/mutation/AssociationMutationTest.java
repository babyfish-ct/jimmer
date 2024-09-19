package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.AuthorProps;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class AssociationMutationTest extends AbstractMutationTest {

    @Test
    public void testInsert() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).checkExistence(false).batchSaveCommand(
                        Arrays.asList(
                            new Tuple2<>(learningGraphQLId1, alexId),
                            new Tuple2<>(learningGraphQLId2, borisId),
                            new Tuple2<>(learningGraphQLId3, borisId)
                        )
                ).checkExistence(true),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) key(BOOK_ID, AUTHOR_ID) values(?, ?)"
                        );
                        it.batchVariables(0, learningGraphQLId1, alexId);
                        it.batchVariables(1, learningGraphQLId2, borisId);
                        it.batchVariables(2, learningGraphQLId3, borisId);
                    });
                    ctx.rowCount(3);
                }
        );
    }

    @Test
    public void testInsertIgnore() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).saveCommand(
                        learningGraphQLId1, alexId
                ).checkExistence(true),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) key(BOOK_ID, AUTHOR_ID) values(?, ?)"
                        );
                        it.variables(learningGraphQLId1, alexId);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testReplace() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).saveCommand(
                        learningGraphQLId1, alexId
                ).checkExistence(true).deleteUnnecessary(true),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID <> ?"
                        );
                        it.variables(learningGraphQLId1, alexId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) key(BOOK_ID, AUTHOR_ID) values(?, ?)"
                        );
                        it.variables(learningGraphQLId1, alexId);
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testDelete() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).batchDeleteCommand(
                        Arrays.asList(
                            new Tuple2<>(learningGraphQLId1, alexId),
                            new Tuple2<>(learningGraphQLId2, alexId),
                            new Tuple2<>(learningGraphQLId3, borisId)
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID = ?"
                        );
                        it.batchVariables(0, learningGraphQLId1, alexId);
                        it.batchVariables(1, learningGraphQLId2, alexId);
                        it.batchVariables(2, learningGraphQLId3, borisId);
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testInverseInsert() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).checkExistence(false).batchSaveCommand(
                        Arrays.asList(
                                new Tuple2<>(alexId, learningGraphQLId1),
                                new Tuple2<>(borisId, learningGraphQLId2),
                                new Tuple2<>(borisId, learningGraphQLId3)
                        )
                ).checkExistence(true),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) " +
                                        "key(AUTHOR_ID, BOOK_ID) values(?, ?)"
                        );
                        it.batchVariables(0, alexId, learningGraphQLId1);
                        it.batchVariables(1, borisId, learningGraphQLId2);
                        it.batchVariables(2, borisId, learningGraphQLId3);
                    });
                    ctx.rowCount(3);
                }
        );
    }

    @Test
    public void testInverseInsertIgnore() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).checkExistence().saveCommand(
                        alexId, learningGraphQLId1
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) " +
                                        "key(AUTHOR_ID, BOOK_ID) values(?, ?)"
                        );
                        it.variables(alexId, learningGraphQLId1);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testInverseReplace() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).checkExistence().deleteUnnecessary().saveCommand(
                        alexId, learningGraphQLId1
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING where AUTHOR_ID = ? and BOOK_ID <> ?"
                        );
                        it.variables(alexId, learningGraphQLId1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) " +
                                        "key(AUTHOR_ID, BOOK_ID) values(?, ?)"
                        );
                        it.variables(alexId, learningGraphQLId1);
                    });
                    ctx.rowCount(3);
                }
        );
    }

    @Test
    public void testInverseDelete() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).batchDeleteCommand(
                        Arrays.asList(
                                new Tuple2<>(alexId, learningGraphQLId1),
                                new Tuple2<>(alexId, learningGraphQLId2),
                                new Tuple2<>(borisId, learningGraphQLId3)
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where AUTHOR_ID = ? and BOOK_ID = ?"
                        );
                        it.batchVariables(0, alexId, learningGraphQLId1);
                        it.batchVariables(1, alexId, learningGraphQLId2);
                        it.batchVariables(2, borisId, learningGraphQLId3);
                    });
                    ctx.rowCount(2);
                }
        );
    }
}
