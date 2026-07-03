package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Asset;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Car;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.CarDraft;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Vehicle;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
