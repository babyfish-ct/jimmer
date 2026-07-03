package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.*;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MultiLevelJoinedInheritanceMutationTest extends AbstractMutationTest {

    private static String joinedAssetRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select a.ASSET_TYPE, a.NAME, v.MANUFACTURER, c.SEAT_COUNT, t.PAYLOAD, d.FORMAT " +
                        "from ML_JOINED_ASSET a " +
                        "left join ML_JOINED_VEHICLE v on a.ID = v.ID " +
                        "left join ML_JOINED_CAR c on a.ID = c.ID " +
                        "left join ML_JOINED_TRUCK t on a.ID = t.ID " +
                        "left join ML_JOINED_DOCUMENT d on a.ID = d.ID " +
                        "where a.ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "[" +
                        rs.getString(1) +
                        ", " +
                        rs.getString(2) +
                        ", " +
                        rs.getString(3) +
                        ", " +
                        rs.getObject(4) +
                        ", " +
                        rs.getObject(5) +
                        ", " +
                        rs.getString(6) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testInsertLeafWritesAllJoinedStages() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                CarDraft.$.produce(car -> {
                                    car.setId(803L);
                                    car.setName("Inserted Car");
                                    car.setManufacturer("Honda");
                                    car.setSeatCount(4);
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ML_JOINED_ASSET(ID, ASSET_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(803L, "CAR", "Inserted Car");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ML_JOINED_VEHICLE(ID, MANUFACTURER) " +
                                        "values(?, ?)"
                        );
                        it.variables(803L, "Honda");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ML_JOINED_CAR(ID, SEAT_COUNT) " +
                                        "values(?, ?)"
                        );
                        it.variables(803L, 4);
                    });
                    ctx.rowCount(AffectedTable.of(Asset.class), 1);
                    ctx.rowCount(AffectedTable.of(Vehicle.class), 1);
                    ctx.rowCount(AffectedTable.of(Car.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":803,\"name\":\"Inserted Car\",\"manufacturer\":\"Honda\",\"seatCount\":4}");
                        it.modified("{\"id\":803,\"name\":\"Inserted Car\",\"manufacturer\":\"Honda\",\"seatCount\":4}");
                    });
                }
        );
    }

    @Test
    public void testDeleteLeafUsesStagedJoinedPath() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Car.class, 800L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedAssetRow(con, 800L) + "; " + joinedAssetRow(con, 801L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from ML_JOINED_CAR where ID = ?");
                        it.variables(800L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ML_JOINED_VEHICLE where ID = ?");
                        it.variables(800L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ML_JOINED_ASSET where ID = ? and ASSET_TYPE = ?");
                        it.variables(800L, "CAR");
                    });
                    ctx.value("null; [TRUCK, Joined Truck, Volvo, null, 12000, null]");
                }
        );
    }

    @Test
    public void testCreateUpdateLeafCanSetIntermediateProp() {
        executeAndExpectRowCount(
                sqlOnlyUpdateJoinClient(1)
                        .createUpdate(CarTable.class, (u, car) -> {
                            u.set(car.manufacturer(), "Honda");
                            u.where(car.seatCount().eq(5));
                        }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ML_JOINED_VEHICLE tb_1_ " +
                                        "set MANUFACTURER = ? " +
                                        "from ML_JOINED_CAR tb_1__sub, ML_JOINED_ASSET tb_1__asset " +
                                        "where tb_1_.ID = tb_1__sub.ID " +
                                        "and tb_1_.ID = tb_1__asset.ID " +
                                        "and tb_1__sub.SEAT_COUNT = ? " +
                                        "and tb_1__asset.ASSET_TYPE = ?"
                        );
                        it.variables("Honda", 5, "CAR");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testCreateUpdateLeafCanSetIntermediatePropByPortableExists() {
        executeAndExpectRowCount(
                h2Client(1)
                        .createUpdate(CarTable.class, (u, car) -> {
                            u.set(car.manufacturer(), "Honda");
                            u.where(car.seatCount().eq(5));
                            u.where(car.manufacturer().eq("Toyota"));
                            u.where(car.name().eq("Joined Car"));
                        }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ML_JOINED_VEHICLE tb_1_ " +
                                        "set MANUFACTURER = ? " +
                                        "where exists(" +
                                        "--->select 1 from ML_JOINED_CAR tb_1__sub " +
                                        "--->where tb_1_.ID = tb_1__sub.ID " +
                                        "--->and tb_1__sub.SEAT_COUNT = ?" +
                                        ") " +
                                        "and tb_1_.MANUFACTURER = ? " +
                                        "and exists(" +
                                        "--->select 1 from ML_JOINED_ASSET tb_1__asset " +
                                        "--->where tb_1_.ID = tb_1__asset.ID " +
                                        "--->and tb_1__asset.NAME = ?" +
                                        ") " +
                                        "and exists(" +
                                        "--->select 1 from ML_JOINED_ASSET tb_1__asset " +
                                        "--->where tb_1_.ID = tb_1__asset.ID " +
                                        "--->and tb_1__asset.ASSET_TYPE = ?" +
                                        ")"
                        );
                        it.variables("Honda", 5, "Toyota", "Joined Car", "CAR");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testCreateUpdateLeafCannotSetIntermediateAndLeafPropsTogether() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getLambdaClient().createUpdate(CarTable.class, (u, car) -> {
                    u.set(car.manufacturer(), "Honda");
                    u.set(car.seatCount(), 4);
                    u.where(car.id().eq(800L));
                })
        );
        assertEquals(
                "Cannot update property \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Car.seatCount" +
                        "\" by createUpdate for joined inheritance type \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Car" +
                        "\" because all assignment targets must belong to the same physical table. " +
                        "Current assignment targets table \"" +
                        "ML_JOINED_CAR" +
                        "\" but previous assignments target table \"" +
                        "ML_JOINED_VEHICLE" +
                        "\". Updating columns in multiple database tables by one createUpdate " +
                        "for joined inheritance requires a dialect that supports multi-table update assignment",
                ex.getMessage()
        );
    }

    private LambdaClient sqlOnlyUpdateJoinClient(int rowCount) {
        return getLambdaClient(it -> {
            it.setDialect(new H2UpdateJoinDialect());
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    return (R) Integer.valueOf(rowCount);
                }

                @Override
                public BatchContext executeBatch(
                        Connection con,
                        String sql,
                        ImmutableProp generatedIdProp,
                        ExecutionPurpose purpose,
                        JSqlClientImplementor sqlClient,
                        boolean constraintViolationTranslatable
                ) {
                    throw new AssertionError("Batch execution is not expected");
                }
            });
        });
    }

    private LambdaClient h2Client(int rowCount) {
        return getLambdaClient(it -> {
            it.setDialect(new H2Dialect());
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    return (R) Integer.valueOf(rowCount);
                }

                @Override
                public BatchContext executeBatch(
                        Connection con,
                        String sql,
                        ImmutableProp generatedIdProp,
                        ExecutionPurpose purpose,
                        JSqlClientImplementor sqlClient,
                        boolean constraintViolationTranslatable
                ) {
                    throw new AssertionError("Batch execution is not expected");
                }
            });
        });
    }

    private static class H2UpdateJoinDialect extends H2Dialect {

        @Override
        public UpdateJoin getUpdateJoin() {
            return new UpdateJoin(false, UpdateJoin.From.AS_JOIN);
        }
    }
}
