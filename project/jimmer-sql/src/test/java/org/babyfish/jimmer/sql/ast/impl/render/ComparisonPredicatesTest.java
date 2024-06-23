package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.ast.tuple.Tuple4;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeProps;
import org.babyfish.jimmer.sql.model.embedded.OrderItemId;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.embedded.ProductId;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

public class ComparisonPredicatesTest extends AbstractQueryTest {

    @Test
    public void testOneSingleValue() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        List<ValueGetter> getters = ValueGetter.valueGetters(
                sqlClient,
                BookProps.STORE.unwrap()
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                Collections.singleton(Constants.manningId),
                "STORE_ID = ?",
                Constants.manningId
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                Collections.singleton(Constants.manningId),
                "STORE_ID <> ?",
                Constants.manningId
        );
    }

    @Test
    public void testAnyEquality() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient(it -> {
            it.setDialect(new H2Dialect() {
                @Override
                public boolean isAnyEqualityOfArraySupported() {
                    return true;
                }
            });
        });
        List<ValueGetter> getters = ValueGetter.valueGetters(
                sqlClient,
                BookProps.STORE.unwrap()
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                Arrays.asList(Constants.oreillyId, Constants.manningId),
                "STORE_ID = any(?)",
                Arrays.asList(Constants.oreillyId, Constants.manningId)
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                Arrays.asList(Constants.oreillyId, Constants.manningId),
                "not (STORE_ID = any(?))",
                Arrays.asList(Constants.oreillyId, Constants.manningId)
        );
    }

    @Test
    public void testMultipleSingleValues() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient(it -> {
            it.setDialect(new H2Dialect() {
                @Override
                public boolean isAnyEqualityOfArraySupported() {
                    return false;
                }
            });
        });
        List<ValueGetter> getters = ValueGetter.valueGetters(
                sqlClient,
                BookProps.STORE.unwrap()
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                Arrays.asList(Constants.oreillyId, Constants.manningId),
                "STORE_ID in (?, ?)",
                Constants.oreillyId, Constants.manningId
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                Arrays.asList(Constants.oreillyId, Constants.manningId),
                "STORE_ID not in (?, ?)",
                Constants.oreillyId, Constants.manningId
        );
    }

    @Test
    public void testNoValues() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        AssociationType type = AssociationType.of(OrderItemProps.PRODUCTS.unwrap());
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                ValueGetter.valueGetters(sqlClient, type.getSourceProp()),
                ValueGetter.valueGetters(sqlClient, type.getTargetProp())
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                Collections.emptyList(),
                "1 = 0"
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                Collections.emptyList(),
                "1 = 1"
        );
    }

    @Test
    public void testOneValue() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        AssociationType type = AssociationType.of(OrderItemProps.PRODUCTS.unwrap());
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                ValueGetter.valueGetters(sqlClient, type.getSourceProp()),
                ValueGetter.valueGetters(sqlClient, type.getTargetProp())
        );
        List<Tuple2<OrderItemId, ProductId>> tuples = Collections.singletonList(
                new Tuple2<>(
                        Objects.createOrderItemId(id -> id.setA(1).setB(4).setC(9)),
                        Objects.createProductId(id -> id.setAlpha("A_").setBeta("B_"))
                )
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                tuples,
                "(" +
                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                        ") = (?, ?, ?, ?, ?)",
                1, 4, 9, "A_", "B_"
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                tuples,
                "(" +
                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                        ") <> (?, ?, ?, ?, ?)",
                1, 4, 9, "A_", "B_"
        );
    }

    @Test
    public void testMultipleValues() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        AssociationType type = AssociationType.of(OrderItemProps.PRODUCTS.unwrap());
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                ValueGetter.valueGetters(sqlClient, type.getSourceProp()),
                ValueGetter.valueGetters(sqlClient, type.getTargetProp())
        );
        List<Tuple2<OrderItemId, ProductId>> tuples = Arrays.asList(
                new Tuple2<>(
                        Objects.createOrderItemId(id -> id.setA(1).setB(4).setC(9)),
                        Objects.createProductId(id -> id.setAlpha("A_").setBeta("B_"))
                ),
                new Tuple2<>(
                        Objects.createOrderItemId(id -> id.setA(2).setB(4).setC(8)),
                        Objects.createProductId(id -> id.setAlpha("aa").setBeta("bb"))
                )
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                tuples,
                "(" +
                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                        ") in (" +
                        "--->(?, ?, ?, ?, ?), " +
                        "--->(?, ?, ?, ?, ?)" +
                        ")",
                1, 4, 9, "A_", "B_", 2, 4, 8, "aa", "bb"
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                tuples,
                "(" +
                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, " +
                        "--->FK_PRODUCT_ALPHA, FK_PRODUCT_BETA" +
                        ") not in (" +
                        "--->(?, ?, ?, ?, ?), " +
                        "--->(?, ?, ?, ?, ?)" +
                        ")",
                1, 4, 9, "A_", "B_", 2, 4, 8, "aa", "bb"
        );
    }

    @Test
    public void testNoTupleDialect() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient(it -> {
            it.setDialect(
                    new H2Dialect() {
                        @Override
                        public boolean isTupleSupported() {
                            return false;
                        }
                    }
            );
        });
        AssociationType type = AssociationType.of(OrderItemProps.PRODUCTS.unwrap());
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                ValueGetter.valueGetters(sqlClient, type.getSourceProp()),
                ValueGetter.valueGetters(sqlClient, type.getTargetProp())
        );
        List<Tuple2<OrderItemId, ProductId>> tuples = Arrays.asList(
                new Tuple2<>(
                        Objects.createOrderItemId(id -> id.setA(1).setB(4).setC(9)),
                        Objects.createProductId(id -> id.setAlpha("A_").setBeta("B_"))
                ),
                new Tuple2<>(
                        Objects.createOrderItemId(id -> id.setA(2).setB(4).setC(8)),
                        Objects.createProductId(id -> id.setAlpha("aa").setBeta("bb"))
                )
        );
        assertSQL(
                sqlClient,
                false,
                getters,
                tuples,
                "(FK_ORDER_ITEM_A = ? and FK_ORDER_ITEM_B = ? and FK_ORDER_ITEM_C = ? and " +
                        "FK_PRODUCT_ALPHA = ? and FK_PRODUCT_BETA = ?" +
                        " or " +
                        "FK_ORDER_ITEM_A = ? and FK_ORDER_ITEM_B = ? and FK_ORDER_ITEM_C = ? and " +
                        "FK_PRODUCT_ALPHA = ? and FK_PRODUCT_BETA = ?)",
                1, 4, 9, "A_", "B_", 2, 4, 8, "aa", "bb"
        );
        assertSQL(
                sqlClient,
                true,
                getters,
                tuples,
                "(" +
                        "--->FK_ORDER_ITEM_A <> ? or FK_ORDER_ITEM_B <> ? or FK_ORDER_ITEM_C <> ? or " +
                        "--->FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?" +
                        ") and (" +
                        "--->FK_ORDER_ITEM_A <> ? or FK_ORDER_ITEM_B <> ? or FK_ORDER_ITEM_C <> ? or " +
                        "--->FK_PRODUCT_ALPHA <> ? or FK_PRODUCT_BETA <> ?" +
                        ")",
                1, 4, 9, "A_", "B_", 2, 4, 8, "aa", "bb"
        );
    }

    private static void assertSQL(
            JSqlClientImplementor sqlClient,
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            String sql,
            Object ... variables
    ) {
        assertSQL(
                sqlClient,
                false,
                negative,
                getters,
                values,
                sql,
                variables
        );
    }

    @Test
    public void testNullable() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient(it -> {
            it.setDialect(new H2Dialect() {
                @Override
                public boolean isAnyEqualityOfArraySupported() {
                    return true;
                }
            });
        });
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                ValueGetter.valueGetters(sqlClient, TreeNodeProps.NAME.unwrap()),
                ValueGetter.valueGetters(sqlClient, TreeNodeProps.PARENT.unwrap())
        );
        List<Tuple2<Object, Object>> tuples = Arrays.asList(
                new Tuple2<>("Food", null),
                new Tuple2<>("Cloth", null),
                new Tuple2<>("Drinks", 1L),
                new Tuple2<>("Bread", 1L),
                new Tuple2<>("Man", 2L),
                new Tuple2<>("Woman", 2L)
        );
        assertSQL(
                sqlClient,
                true,
                false,
                getters,
                tuples,
                "(" +
                        "--->(NAME, PARENT_ID) in ((?, ?), (?, ?), (?, ?), (?, ?)) or " +
                        "--->PARENT_ID is null and NAME = any(?)" +
                        ")",
                "Drinks", 1L, "Bread", 1L, "Man", 2L, "Woman", 2L,
                new TypedList<>("bigint", new Object[]{ "Food", "Cloth" })
        );
        assertSQL(
                sqlClient,
                true,
                true,
                getters,
                tuples,
                "(NAME, PARENT_ID) not in ((?, ?), (?, ?), (?, ?), (?, ?)) and " +
                        "(PARENT_ID is not null or not (NAME = any(?)))",
                "Drinks", 1L, "Bread", 1L, "Man", 2L, "Woman", 2L,
                new TypedList<>("bigint", new Object[]{ "Food", "Cloth" })
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNullableEmbedded() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient(it -> {
            it.setDialect(new H2Dialect() {
                @Override
                public boolean isAnyEqualityOfArraySupported() {
                    return false;
                }
            });
        });
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                new List[]{
                        ValueGetter.valueGetters(sqlClient, BookProps.NAME.unwrap()),
                        ValueGetter.valueGetters(sqlClient, BookProps.EDITION.unwrap()),
                        ValueGetter.valueGetters(sqlClient, BookProps.PRICE.unwrap()),
                        ValueGetter.valueGetters(sqlClient, BookProps.STORE.unwrap())
                }
        );
        List<Tuple4<String, Integer, BigDecimal, UUID>> tuples = Arrays.asList(
                new Tuple4<>("GraphQL in Action", null, null, Constants.manningId),
                new Tuple4<>("Learning GraphQL", null, null, Constants.oreillyId),
                new Tuple4<>(null, 1, new BigDecimal("49.9"), Constants.manningId),
                new Tuple4<>(null, 1, new BigDecimal("59.9"), Constants.oreillyId),
                new Tuple4<>("Effective TypeScript", 1, new BigDecimal("49.8"), null),
                new Tuple4<>("Effective TypeScript", 2, new BigDecimal("48.9"), null),
                new Tuple4<>("Programming TypeScript", 2, null, null),
                new Tuple4<>("Programming TypeScript", 3, null, null)
        );
        assertSQL(
                sqlClient,
                true,
                false,
                getters,
                tuples,
                "(" +
                        "--->STORE_ID is null " +
                        "--->and " +
                        "--->(NAME, EDITION, PRICE) in ((?, ?, ?), (?, ?, ?)) " +
                        "--->or NAME is null " +
                        "--->and " +
                        "--->(EDITION, PRICE, STORE_ID) in ((?, ?, ?), (?, ?, ?)) " +
                        "--->or PRICE is null " +
                        "--->and " +
                        "--->(" +
                        "--->--->STORE_ID is null " +
                        "--->--->and " +
                        "--->--->(NAME, EDITION) in ((?, ?), (?, ?)) " +
                        "--->--->or " +
                        "--->--->EDITION is null " +
                        "--->--->and (NAME, STORE_ID) in ((?, ?), (?, ?))" +
                        "--->)" +
                        ")",
                "Effective TypeScript", 1, new BigDecimal("49.8"),
                "Effective TypeScript", 2, new BigDecimal("48.9"),
                1, new BigDecimal("49.9"), Constants.manningId,
                1, new BigDecimal("59.9"), Constants.oreillyId,
                "Programming TypeScript", 2,
                "Programming TypeScript", 3,
                "GraphQL in Action", Constants.manningId,
                "Learning GraphQL", Constants.oreillyId
        );
        assertSQL(
                sqlClient,
                true,
                true,
                getters,
                tuples,
                "(" +
                        "--->STORE_ID is not null or " +
                        "--->(NAME, EDITION, PRICE) not in ((?, ?, ?), (?, ?, ?))" +
                        ") and (" +
                        "--->NAME is not null or " +
                        "--->(EDITION, PRICE, STORE_ID) not in ((?, ?, ?), (?, ?, ?))" +
                        ") and (" +
                        "--->PRICE is not null or " +
                        "--->(" +
                        "--->--->STORE_ID is not null or " +
                        "--->--->(NAME, EDITION) not in ((?, ?), (?, ?))" +
                        "--->) and (" +
                        "--->--->EDITION is not null or " +
                        "--->--->(NAME, STORE_ID) not in ((?, ?), (?, ?))" +
                        "--->)" +
                        ")",
                "Effective TypeScript", 1, new BigDecimal("49.8"),
                "Effective TypeScript", 2, new BigDecimal("48.9"),
                1, new BigDecimal("49.9"), Constants.manningId,
                1, new BigDecimal("59.9"), Constants.oreillyId,
                "Programming TypeScript", 2,
                "Programming TypeScript", 3,
                "GraphQL in Action", Constants.manningId,
                "Learning GraphQL", Constants.oreillyId
        );
    }

    private static void assertSQL(
            JSqlClientImplementor sqlClient,
            boolean nullable,
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            String sql,
            Object ... variables
    ) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        if (nullable) {
            ComparisonPredicates.renderNullableIn(
                    negative,
                    getters,
                    values,
                    builder
            );
        } else {
            ComparisonPredicates.renderIn(
                    negative,
                    getters,
                    values,
                    builder
            );
        }
        Tuple3<String, List<Object>, ?> sqlTuple = builder.build();
        Assertions.assertEquals(sql.replace("--->", ""), sqlTuple.get_1());
        Assertions.assertEquals(Arrays.asList(variables), sqlTuple.get_2());
    }
}
