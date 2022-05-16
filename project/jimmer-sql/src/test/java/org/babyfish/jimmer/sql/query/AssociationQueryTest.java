package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class AssociationQueryTest extends AbstractQueryTest {

    @Test()
    public void test() {

//        executeAndExpect(
//                getSqlClient().createAssociationQuery(BookTable.Ex.class, BookTable.Ex::authors, (q, t) -> {
//                    q.where(t.source().name().eq("Learning GraphQL"));
//                    q.where(t.target().firstName().eq("Alex"));
//                    return q.select(t);
//                }),
//                ctx -> {
//
//                }
//        );
    }
}
