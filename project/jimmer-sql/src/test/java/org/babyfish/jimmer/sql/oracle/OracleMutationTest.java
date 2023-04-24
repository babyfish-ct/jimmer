package org.babyfish.jimmer.sql.oracle;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

public class OracleMutationTest extends AbstractMutationTest {

    @Test
    public void testSave() {

        NativeDatabases.assumeOracleDatabase();

        setAutoIds(BookStore.class, UUID.fromString("885182f9-933a-4d0c-a53b-23b9475483f8"));
        setAutoIds(Book.class, UUID.fromString("96cb139a-7ebe-41a4-9689-3455764b444b"));
        setAutoIds(Author.class, UUID.fromString("53ec4e75-0264-409e-8435-2d91d7756732"), UUID.fromString("19326e82-2c55-458a-8473-2306f1ec873d"));

        executeAndExpectResult(
                NativeDatabases.ORACLE_DATA_SOURCE,
                getSqlClient(cfg -> {
                    UserIdGenerator<?> idGenerator = this::autoId;
                    cfg
                            .setDialect(new OracleDialect())
                            .addScalarProvider(ScalarProvider.UUID_BY_STRING)
                            .setIdGenerator(idGenerator);
                })
                        .getEntities().saveCommand(
                                BookDraft.$.produce(book -> {
                                    book.setName("SQL in Action")
                                            .setEdition(1)
                                            .setPrice(new BigDecimal(49))
                                            .applyStore(store -> {
                                                store.setName("TURING");
                                            })
                                            .addIntoAuthors(author -> {
                                                author.setFirstName("Tim")
                                                        .setLastName("Cook")
                                                        .setGender(Gender.MALE);
                                            })
                                            .addIntoAuthors(author -> {
                                                author.setFirstName("Linda")
                                                        .setLastName("White")
                                                        .setGender(Gender.FEMALE);
                                            });
                                })
                        ).setAutoAttachingAll(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.NAME = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.EDITION = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "select ?, ? from dual " +
                                        "union all " +
                                        "select ?, ? from dual"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"name\":\"SQL in Action\"," +
                                        "--->\"edition\":1," +
                                        "--->\"price\":49," +
                                        "--->\"store\":{" +
                                        "--->--->\"name\":\"TURING\"" +
                                        "--->}," +
                                        "--->\"authors\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"firstName\":\"Tim\"," +
                                        "--->--->--->\"lastName\":\"Cook\"," +
                                        "--->--->--->\"gender\":\"MALE\"" +
                                        "--->--->}," +
                                        "--->--->{" +
                                        "--->--->--->\"firstName\":\"Linda\"," +
                                        "--->--->--->\"lastName\":\"White\"," +
                                        "--->--->--->\"gender\":\"FEMALE\"" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":\"96cb139a-7ebe-41a4-9689-3455764b444b\"," +
                                        "--->\"name\":\"SQL in Action\"," +
                                        "--->\"edition\":1," +
                                        "--->\"price\":49," +
                                        "--->\"store\":{" +
                                        "--->--->\"id\":\"885182f9-933a-4d0c-a53b-23b9475483f8\"," +
                                        "--->--->\"name\":\"TURING\"," +
                                        "--->--->\"version\":0" +
                                        "--->}," +
                                        "--->\"authors\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"53ec4e75-0264-409e-8435-2d91d7756732\"," +
                                        "--->--->--->\"firstName\":\"Tim\"," +
                                        "--->--->--->\"lastName\":\"Cook\"," +
                                        "--->--->--->\"gender\":\"MALE\"" +
                                        "--->--->}," +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"19326e82-2c55-458a-8473-2306f1ec873d\"," +
                                        "--->--->--->\"firstName\":\"Linda\"," +
                                        "--->--->--->\"lastName\":\"White\"," +
                                        "--->--->--->\"gender\":\"FEMALE\"" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Author.class), 2);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 2);
                }
        );
    }
}
