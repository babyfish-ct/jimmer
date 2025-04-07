package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.mutation.Rule;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class RuleTest extends AbstractMutationTest {

    @Test
    public void test() {
        List<Book> books = new ArrayList<>();
        getSqlClient()
                .saveEntitiesCommand(books)
                .setRule(
                        Rule.insert(BookProps.EDITION)
                                .whenUnloaded(1)
                )
                .setRule(
                        Rule.update(BookProps.PRICE).of(BookTable.class)
                                .whenLoaded(c ->
                                    c.table().price().plus(c.value(BookProps.PRICE))
                                )
                )
                .setRule(
                        Rule.update(BookProps.STORE)
                                .dot(BookStoreProps.ID)
                                .of(BookTable.class)
                                .never()
                )
                .setRule(
                        Rule.update(BookTable.class)
                                .where(c -> {
                                    return c.table().edition().le(c.value(BookProps.EDITION));
                                })
                )
                .execute();
    }

}
