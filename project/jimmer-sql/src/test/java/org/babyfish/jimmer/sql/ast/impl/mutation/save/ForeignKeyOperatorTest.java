package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class ForeignKeyOperatorTest extends AbstractMutationTest {

    @Test
    public void testSimple() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("8c3c998b-f926-49ec-82c0-b2f6291715ea"));
            draft.setStoreId(manningId);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setStoreId(manningId);
        });
        execute(
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    ForeignKeyOperator operator = operator(getSqlClient(), con, BookStoreProps.BOOKS.unwrap());
                    return operator.disconnectExcept(drafts);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK tb_1_ set STORE_ID = null " +
                                        "where tb_1_.STORE_ID = ? and tb_1_.ID not in (?, ?)"
                        );
                        it.variables(manningId, book1.id(), book2.id());
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testEmbeddable() {
        OrderItem orderItem1 = OrderItemDraft.$.produce(draft -> {
            draft.setId(Objects.createOrderItemId(id -> id.setA(1).setB(1).setC(1)));
            draft.setOrderId(Objects.createOrderId(id -> id.setX("001").setY("002")));
        });
        OrderItem orderItem2 = OrderItemDraft.$.produce(draft -> {
            draft.setId(Objects.createOrderItemId(id -> id.setA(1).setB(2).setC(1)));
            draft.setOrderId(Objects.createOrderId(id -> id.setX("001").setY("001")));
        });
        execute(
                new OrderItem[] { orderItem1, orderItem2 },
                (con, drafts) -> {
                    ForeignKeyOperator operator = operator(getSqlClient(), con, OrderProps.ORDER_ITEMS.unwrap());
                    return operator.disconnectExcept(drafts);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ORDER_ITEM tb_1_ set " +
                                        "FK_ORDER_X = null, FK_ORDER_Y = null " +
                                        "where (tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) in ((?, ?), (?, ?)) " +
                                        "and (" +
                                        "--->tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C" +
                                        ") not in (" +
                                        "--->(?, ?, ?), (?, ?, ?)" +
                                        ")"
                        );
                        it.variables("001", "002", "001", "001", 1, 1, 1, 1, 2, 1);
                    });
                    ctx.value("2");
                }
        );
    }

    @SuppressWarnings("unchecked")
    private <T, R> void execute(
            T[] entities,
            BiFunction<Connection, List<DraftSpi>, R> block,
            Consumer<ExpectDSLWithValue<R>> ctxBlock
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

    private static ForeignKeyOperator operator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp oneToManyProp
    ) {
        SaveOptionsImpl options = new SaveOptionsImpl((JSqlClientImplementor) sqlClient);
        return new ForeignKeyOperator(
                new SaveContext(
                        options,
                        con,
                        oneToManyProp.getDeclaringType()
                ).to(oneToManyProp)
        );
    }

    private static byte[] toByteArray(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }
}
