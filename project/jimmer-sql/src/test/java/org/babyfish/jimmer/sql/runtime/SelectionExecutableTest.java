package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.babyfish.jimmer.support.JdbcRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId1;

public class SelectionExecutableTest extends AbstractQueryTest {

    @Test
    public void testQueryExecuteUsesLocalJdbcOptions() {
        BookTable book = BookTable.$;
        jdbc(con -> {
            JdbcRecorder recorder = new JdbcRecorder(con);
            getSqlClient()
                    .createQuery(book)
                    .jdbcFetchSize(128)
                    .jdbcQueryTimeout(16)
                    .where(book.id().eq(learningGraphQLId1))
                    .select(book.id(), book.name())
                    .execute(recorder.connection());
            Assertions.assertEquals("[128]", recorder.fetchSizes().toString());
            Assertions.assertEquals("[16]", recorder.queryTimeouts().toString());
        });
    }

    @Test
    public void testQueryStreamUsesLocalJdbcOptionsAndClosesResources() {
        BookTable book = BookTable.$;
        jdbc(con -> {
            JdbcRecorder recorder = new JdbcRecorder(con);
            try (Stream<Tuple2<UUID, String>> ignored = getSqlClient()
                    .createQuery(book)
                    .jdbcFetchSize(128)
                    .jdbcQueryTimeout(16)
                    .where(book.id().eq(learningGraphQLId1))
                    .select(book.id(), book.name())
                    .stream(recorder.connection())
            ) {
                // Closing the stream must close the JDBC cursor resources even if rows are not consumed.
            }
            Assertions.assertEquals("[128]", recorder.fetchSizes().toString());
            Assertions.assertEquals("[16]", recorder.queryTimeouts().toString());
            Assertions.assertEquals(1, recorder.resultSetCloseCount());
            Assertions.assertEquals(1, recorder.statementCloseCount());
        });
    }

    @Test
    public void testStreamFetcherWithJoinFetchedAssociation() {
        BookTable book = BookTable.$;
        connectAndExpect(
                con -> {
                    try (Stream<Book> stream = getSqlClient()
                            .createQuery(book)
                            .where(book.name().eq("GraphQL in Action"))
                            .orderBy(book.edition())
                            .select(
                                    book.fetch(
                                            BookFetcher.$
                                                    .name()
                                                    .store(
                                                            ReferenceFetchType.JOIN_ALWAYS,
                                                            BookStoreFetcher.$.name()
                                                    )
                                    )
                            )
                            .stream(con)
                    ) {
                        return stream.collect(Collectors.toList());
                    }
                },
                ctx -> {
                    ctx.rows(
                            "[" +
                                    "--->{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\",\"name\":\"GraphQL in Action\",\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\"}}," +
                                    "--->{\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\",\"name\":\"GraphQL in Action\",\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\"}}," +
                                    "--->{\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\",\"name\":\"GraphQL in Action\",\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\"}}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testStreamFetcherRejectsSelectFetchedAssociation() {
        BookTable book = BookTable.$;
        jdbc(con -> {
            UnsupportedOperationException ex = Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> {
                        try (Stream<Book> ignored = getSqlClient()
                                .createQuery(book)
                                .select(
                                        book.fetch(
                                                BookFetcher.$
                                                        .name()
                                                        .store(
                                                                ReferenceFetchType.SELECT,
                                                                BookStoreFetcher.$.name()
                                                        )
                                        )
                                )
                                .stream(con)
                        ) {
                            // The stream must not be created because this fetcher requires post-fetch.
                        }
                    }
            );
            Assertions.assertTrue(ex.getMessage().contains("post-fetch"));
        });
    }

    @Test
    public void testStreamTupleWithEntityAndEmbeddable() {
        MachineTable machine = MachineTable.$;
        connectAndExpect(
                con -> {
                    try (Stream<Tuple2<Machine, Location>> stream = getSqlClient()
                            .createQuery(machine)
                            .select(
                                    machine.fetch(MachineFetcher.$.cpuFrequency()),
                                    machine.location().fetch(LocationFetcher.$.host().port())
                            )
                            .stream(con)
                    ) {
                        return stream.collect(Collectors.toList());
                    }
                },
                ctx -> {
                    ctx.rows(rows -> {
                        Assertions.assertEquals(1, rows.size());
                        assertContentEquals(
                                "Tuple2(_1={\"id\":1,\"cpuFrequency\":2}, _2={\"host\":\"localhost\",\"port\":8080})",
                                rows.get(0)
                        );
                    });
                }
        );
    }

    @Test
    public void testDefaultJdbcOptions() {
        BookTable book = BookTable.$;
        jdbc(con -> {
            JdbcRecorder recorder = new JdbcRecorder(con);
            getSqlClient(it -> {
                it.setDefaultJdbcFetchSize(256);
                it.setDefaultJdbcQueryTimeout(32);
            })
                    .createQuery(book)
                    .where(book.id().eq(learningGraphQLId1))
                    .select(book.id(), book.name())
                    .execute(recorder.connection());
            Assertions.assertEquals("[256]", recorder.fetchSizes().toString());
            Assertions.assertEquals("[32]", recorder.queryTimeouts().toString());
        });
    }

    @Test
    public void testLocalJdbcOptionZeroOverridesDefault() {
        BookTable book = BookTable.$;
        jdbc(con -> {
            JdbcRecorder recorder = new JdbcRecorder(con);
            getSqlClient(it -> {
                it.setDefaultJdbcFetchSize(256);
                it.setDefaultJdbcQueryTimeout(32);
            })
                    .createQuery(book)
                    .jdbcFetchSize(0)
                    .jdbcQueryTimeout(0)
                    .where(book.id().eq(learningGraphQLId1))
                    .select(book.id(), book.name())
                    .execute(recorder.connection());
            Assertions.assertEquals("[0]", recorder.fetchSizes().toString());
            Assertions.assertEquals("[0]", recorder.queryTimeouts().toString());
        });
    }

    @Test
    public void testUpdateReturningUsesLocalJdbcOptions() {
        BookTable book = BookTable.$;
        jdbc(con -> {
            JdbcRecorder recorder = new JdbcRecorder(con);
            getSqlClient()
                    .createUpdate(book)
                    .jdbcFetchSize(128)
                    .jdbcQueryTimeout(16)
                    .set(book.name(), "Learning GraphQL+")
                    .where(book.id().eq(learningGraphQLId1))
                    .returning(book.id(), book.name())
                    .execute(recorder.connection());
            Assertions.assertEquals("[128]", recorder.fetchSizes().toString());
            Assertions.assertEquals("[16]", recorder.queryTimeouts().toString());
        });
    }

    @Test
    public void testJdbcOptionValidation() {
        BookTable book = BookTable.$;
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .createQuery(book)
                        .jdbcFetchSize(-1)
                        .select(book.id())
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .createUpdate(book)
                        .jdbcQueryTimeout(-1)
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient(it -> it.setDefaultJdbcFetchSize(0))
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient(it -> it.setDefaultJdbcFetchSize(-1))
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient(it -> it.setDefaultJdbcQueryTimeout(0))
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient(it -> it.setDefaultJdbcQueryTimeout(-1))
        );
    }

}
