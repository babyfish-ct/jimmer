package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class OperatorTest extends AbstractMutationTest {

    private static final Set<ImmutableProp> BOOK_KEY_PROPS = new LinkedHashSet<>(
            Arrays.asList(
                    BookProps.NAME.unwrap(),
                    BookProps.EDITION.unwrap()
            )
    );

    @Test
    public void testInsert() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("8c3c998b-f926-49ec-82c0-b2f6291715ea"));
            draft.setName("SQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4"));
            draft.setName("Kotlin in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("49.9"));
        });
        execute(
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Book.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    return operator.insert(shapedEntityMap.iterator().next());
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE)values(?, ?, ?, ?)");
                        it.batchVariables(
                                0,
                                UUID.fromString("8c3c998b-f926-49ec-82c0-b2f6291715ea"),
                                "SQL in Action",
                                2,
                                new BigDecimal("59.9")
                        );
                        it.batchVariables(
                                1,
                                UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4"),
                                "Kotlin in Action",
                                1,
                                new BigDecimal("49.9")
                        );
                    });
                    ctx.value("2");
                }
        );
    }

    @SuppressWarnings("unchecked")
    private <T, R> void execute(
            T[] entities,
            BiFunction<Connection, List<DraftSpi>, R> block,
            Consumer<AbstractMutationTest.ExpectDSLWithValue<R>> ctxBlock
    ) {
        Internal.produceList(
                ImmutableType.get(entities.getClass().getComponentType()),
                Arrays.asList(entities),
                drafts -> {
                    connectAndExpect(
                            con -> {
                                return block.apply(con, (List<DraftSpi>) drafts);
                            },
                            ctxBlock
                    );
                }
        );
    }

    private static Operator operator(
            JSqlClient sqlClient,
            Connection con,
            Class<?> entityType
    ) {
        return new Operator(
                new SaveContext(
                        new SaveOptionsImpl((JSqlClientImplementor) sqlClient),
                        con,
                        ImmutableType.get(entityType)
                )
        );
    }
}
