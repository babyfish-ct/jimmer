package org.babyfish.jimmer.sql.microservice;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.model.microservice.Order;
import org.babyfish.jimmer.sql.model.microservice.OrderDraft;
import org.babyfish.jimmer.sql.model.microservice.OrderItemDraft;
import org.babyfish.jimmer.sql.model.microservice.ProductDraft;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

public class MicroServiceMutationTest extends AbstractMutationTest {

    @Test
    public void testSaveManyToOneWithId() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-item-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderItemDraft.$.produce(item -> {
                            item.setId(100L);
                            item.setName("new-item");
                            item.setOrder(ImmutableObjects.makeIdOnly(Order.class, 1L));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from MS_ORDER_ITEM tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MS_ORDER_ITEM(ID, NAME, ORDER_ID) values(?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original("{\"id\":100,\"name\":\"new-item\",\"order\":{\"id\":1}}");
                        it.modified("{\"id\":100,\"name\":\"new-item\",\"order\":{\"id\":1}}");
                    });
                }
        );
    }

    @Test
    public void testSaveManyToOneWithIllegalId() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-item-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderItemDraft.$.produce(item -> {
                            item.setId(100L);
                            item.setName("new-item");
                            item.setOrder(ImmutableObjects.makeIdOnly(Order.class, 10L));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.throwable(it -> {
                        it.type(SaveException.class);
                        it.message("Save error caused by the path: \"<root>.order\": Illegal ids: [10]");
                    });
                }
        );
    }

    @Test
    public void testSaveManyToOneWithNonIdValue() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-item-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderItemDraft.$.produce(item -> {
                            item.setId(100L);
                            item.setName("new-item");
                            item.applyOrder(order -> order.setName("order"));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.throwable(it -> {
                        it.type(SaveException.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "The property \"org.babyfish.jimmer.sql.model.microservice.OrderItem.order\" " +
                                        "is remote(across different microservices) association, " +
                                        "but it has associated object which is not id-only"
                        );
                    });
                }
        );
    }

    @Test
    public void testSaveOneToMany() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderDraft.$.produce(order -> {
                            order.setId(100L);
                            order.setName("new-order");
                            order.addIntoOrderItems(item -> item.setId(1L));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.statement(it -> {});
                    ctx.statement(it -> {});
                    ctx.throwable(it -> {
                        it.type(SaveException.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "The property \"org.babyfish.jimmer.sql.model.microservice.Order.orderItems\" " +
                                        "which is reversed(with `mappedBy`) remote(across different microservices) association " +
                                        "cannot be supported by save command"
                        );
                    });
                }
        );
    }

    @Test
    public void testSaveManyToManyWithId() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-item-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderItemDraft.$.produce(item -> {
                            item.setId(100L);
                            item.setName("new-item");
                            item.applyOrder(order -> order.setId(1L));
                            item.addIntoProducts(product -> product.setId(1L));
                            item.addIntoProducts(product -> product.setId(3L));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from MS_ORDER_ITEM tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MS_ORDER_ITEM(ID, NAME, ORDER_ID) values(?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MS_ORDER_ITEM_PRODUCT_MAPPING(ORDER_ITEM_ID, PRODUCT_ID) " +
                                        "values (?, ?), (?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"id\":100," +
                                        "--->\"name\":\"new-item\"," +
                                        "--->\"order\":{\"id\":1}," +
                                        "--->\"products\":[{\"id\":1},{\"id\":3}]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":100," +
                                        "--->\"name\":\"new-item\"," +
                                        "--->\"order\":{\"id\":1}," +
                                        "--->\"products\":[{\"id\":1},{\"id\":3}]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testSaveManyToManyWithIllegalId() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-item-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderItemDraft.$.produce(item -> {
                            item.setId(100L);
                            item.setName("new-item");
                            item.applyOrder(order -> order.setId(1L));
                            item.addIntoProducts(product -> product.setId(1L));
                            item.addIntoProducts(product -> product.setId(3L));
                            item.addIntoProducts(product -> product.setId(4L));
                            item.addIntoProducts(product -> product.setId(5L));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.statement(it -> {});
                    ctx.statement(it -> {});
                    ctx.throwable(it -> {
                        it.type(SaveException.class);
                        it.message(
                                "Save error caused by the path: \"<root>.products\": Illegal ids: [4, 5]"
                        );
                    });
                }
        );
    }

    @Test
    public void testSaveManyToManyWithNonIdValue() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("order-item-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        OrderItemDraft.$.produce(item -> {
                            item.setId(100L);
                            item.setName("new-item");
                            item.applyOrder(order -> order.setId(1L));
                            item.addIntoProducts(product -> product.setName("a"));
                            item.addIntoProducts(product -> product.setName("b"));
                        })
                ).configure(AbstractEntitySaveCommand.Cfg::setAutoIdOnlyTargetCheckingAll),
                ctx -> {
                    ctx.statement(it -> {});
                    ctx.statement(it -> {});
                    ctx.throwable(it -> {
                        it.type(SaveException.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "The property \"org.babyfish.jimmer.sql.model.microservice.OrderItem.products\" " +
                                        "is remote(across different microservices) association, " +
                                        "but it has associated object which is not id-only"
                        );
                    });
                }
        );
    }

    @Test
    public void saveReversedManyToMany() {
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg
                            .setMicroServiceName("product-service")
                            .setMicroServiceExchange(new MicroServiceExchangeImpl());
                }).getEntities().saveCommand(
                        ProductDraft.$.produce(product -> {
                            product.setId(1L);
                            product.setName("Mac M1");
                            product.addIntoOrderItems(item -> {
                                item.setId(1L);
                            });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {});
                    ctx.statement(it -> {});
                    ctx.throwable(it -> {
                        it.type(SaveException.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "The property \"org.babyfish.jimmer.sql.model.microservice.Product.orderItems\" " +
                                        "which is reversed(with `mappedBy`) remote(across different microservices) association " +
                                        "cannot be supported by save command"
                        );
                    });
                }
        );
    }
}
