package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookProps;
import org.junit.jupiter.api.Test;

public class QueryByExampleTest extends AbstractQueryTest {

    @Test
    public void testMatchEmpty() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_"
            ).variables();
        });

        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("X");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables("X");
        });

        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book3).match(Example.MatchMode.NOT_NULL)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables("");
        });
    }

    @Test
    public void testMatchNull() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setStore(null);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_"
            ).variables();
        });

        Book book2 = BookDraft.$.produce(draft -> {
            draft.setStore(null);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2).match(Example.MatchMode.NULLABLE)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.STORE_ID is null"
            ).variables();
        });
    }

    @Test
    public void testTrim() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName(" X ");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables(" X ");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName(" X ");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2).trim()
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables("X");
        });
    }

    @Test
    public void testMatchPropEmpty() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_"
            ).variables();
        });

        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("X");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables("X");
        });

        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book3).match(BookProps.NAME, Example.MatchMode.NOT_NULL)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables("");
        });
    }

    @Test
    public void testMatchPropNull() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setStore(null);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_"
            ).variables();
        });

        Book book2 = BookDraft.$.produce(draft -> {
            draft.setStore(null);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2).match(BookProps.STORE, Example.MatchMode.NULLABLE)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.STORE_ID is null"
            ).variables();
        });
    }

    @Test
    public void testPropTrim() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName(" X ");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables(" X ");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName(" X ");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2).trim(BookProps.NAME)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME = ?"
            ).variables("X");
        });
    }

    @Test
    public void testPropZero() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setEdition(0);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.EDITION = ?"
            ).variables(0);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setEdition(0);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book2).ignoreZero(BookProps.EDITION)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_"
            ).variables();
        });
    }

    @Test
    public void testLike() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("G");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1).like(BookProps.NAME, LikeMode.START)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME like ?"
            ).variables("G%");
        });

        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("G");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book1).ilike(BookProps.NAME, LikeMode.START)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where lower(tb_1_.NAME) like ?"
            ).variables("g%");
        });
    }
}
