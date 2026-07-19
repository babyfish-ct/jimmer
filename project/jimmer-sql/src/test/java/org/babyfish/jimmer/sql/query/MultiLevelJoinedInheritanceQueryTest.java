package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MultiLevelJoinedInheritanceQueryTest extends AbstractQueryTest {

    @Test
    public void testInstanceOfIntermediateType() {
        AssetTable table = AssetTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.instanceOf(Vehicle.class))
                        .orderBy(table.id())
                        .select(table.id(), table.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from ML_JOINED_ASSET tb_1_ " +
                                    "where tb_1_.ASSET_TYPE in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK");
                    ctx.row(0, row -> {
                        assertEquals(800L, row.get_1());
                        assertEquals("Joined Car", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(801L, row.get_1());
                        assertEquals("Joined Truck", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithIntermediateAndLeafTypeBranches() {
        AssetTable table = AssetTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(800L, 801L, 802L)))
                        .orderBy(table.id())
                        .select(
                                table.fetch(
                                        AssetFetcher.$
                                                .name()
                                                .forType(VehicleFetcher.$.manufacturer())
                                                .forType(CarFetcher.$.seatCount())
                                                .forType(DocumentFetcher.$.format())
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.ASSET_TYPE, tb_1_.NAME, " +
                                    "tb_2_.MANUFACTURER, tb_3_.SEAT_COUNT, tb_4_.FORMAT " +
                                    "from ML_JOINED_ASSET tb_1_ " +
                                    "left join ML_JOINED_VEHICLE tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.ASSET_TYPE in (?, ?) " +
                                    "left join ML_JOINED_CAR tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_1_.ASSET_TYPE = ? " +
                                    "left join ML_JOINED_DOCUMENT tb_4_ " +
                                    "on tb_1_.ID = tb_4_.ID and tb_1_.ASSET_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK", "CAR", "DOC", 800L, 801L, 802L);
                    ctx.row(0, row -> {
                        assertEquals(Car.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals("Joined Car", row.name());
                        assertEquals("Toyota", ((Vehicle) row).manufacturer());
                        assertEquals(5, ((Car) row).seatCount());
                        assertLoadState(row, "id", "name", "manufacturer", "seatCount");
                    });
                    ctx.row(1, row -> {
                        assertEquals(Truck.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals("Joined Truck", row.name());
                        assertEquals("Volvo", ((Vehicle) row).manufacturer());
                        assertLoadState(row, "id", "name", "manufacturer");
                    });
                    ctx.row(2, row -> {
                        assertEquals(Document.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals("Joined Manual", row.name());
                        assertEquals("PDF", ((Document) row).format());
                        assertLoadState(row, "id", "name", "format");
                    });
                }
        );
    }

    @Test
    public void testPostFetchIsBatchedAtEachHierarchyLevel() {
        AssetTable table = AssetTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(800L, 801L, 802L)))
                        .orderBy(table.id())
                        .select(
                                table.fetch(
                                        AssetFetcher.$
                                                .manager(
                                                        ReferenceFetchType.SELECT,
                                                        VehicleOwnerFetcher.$.name()
                                                )
                                                .forType(
                                                        VehicleFetcher.$.owner(
                                                                ReferenceFetchType.SELECT,
                                                                VehicleOwnerFetcher.$.name()
                                                        )
                                                )
                                                .forType(
                                                        CarFetcher.$.driver(
                                                                ReferenceFetchType.SELECT,
                                                                VehicleOwnerFetcher.$.name()
                                                        )
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.ASSET_TYPE, tb_1_.MANAGER_ID, " +
                                    "tb_2_.OWNER_ID, tb_3_.DRIVER_ID " +
                                    "from ML_JOINED_ASSET tb_1_ " +
                                    "left join ML_JOINED_VEHICLE tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.ASSET_TYPE in (?, ?) " +
                                    "left join ML_JOINED_CAR tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_1_.ASSET_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK", "CAR", 800L, 801L, 802L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from ML_JOINED_VEHICLE_OWNER tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(910L, 911L, 912L);
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from ML_JOINED_VEHICLE_OWNER tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(900L, 901L);
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from ML_JOINED_VEHICLE_OWNER tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(920L);
                    ctx.row(0, row -> {
                        assertEquals("Car Manager", row.manager().name());
                        assertEquals("Car Owner", ((Vehicle) row).owner().name());
                        assertEquals("Car Driver", ((Car) row).driver().name());
                        assertLoadState(row, "id", "manager", "owner", "driver");
                    });
                    ctx.row(1, row -> {
                        assertEquals("Truck Manager", row.manager().name());
                        assertEquals("Truck Owner", ((Vehicle) row).owner().name());
                        assertLoadState(row, "id", "manager", "owner");
                    });
                    ctx.row(2, row -> {
                        assertEquals("Document Manager", row.manager().name());
                        assertLoadState(row, "id", "manager");
                    });
                }
        );
    }

    @Test
    public void testTreatAsIntermediateThenLeaf() {
        AssetTable table = AssetTable.$;
        VehicleTable vehicle = table.tryTreatAs(VehicleTable.class);
        CarTable car = vehicle.tryTreatAs(CarTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(800L, 802L)))
                        .orderBy(table.id())
                        .select(table.id(), vehicle.manufacturer(), car.seatCount()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.MANUFACTURER, tb_3_.SEAT_COUNT " +
                                    "from ML_JOINED_ASSET tb_1_ " +
                                    "left join ML_JOINED_VEHICLE tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.ASSET_TYPE in (?, ?) " +
                                    "left join ML_JOINED_CAR tb_3_ " +
                                    "on tb_2_.ID = tb_3_.ID and tb_1_.ASSET_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK", "CAR", 800L, 802L);
                    ctx.row(0, row -> {
                        assertEquals(800L, row.get_1());
                        assertEquals("Toyota", row.get_2());
                        assertEquals(5, row.get_3());
                    });
                    ctx.row(1, row -> {
                        assertEquals(802L, row.get_1());
                        assertNull(row.get_2());
                        assertNull(row.get_3());
                    });
                }
        );
    }

    @Test
    public void testLeafIdViewDeclaredInIntermediateType() {
        CarTable table = CarTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(800L))
                        .select(table.ownerId()),
                ctx -> {
                    ctx.sql(
                            "select tb_1__vehicle.OWNER_ID " +
                                    "from ML_JOINED_ASSET tb_1_ " +
                                    "inner join ML_JOINED_VEHICLE tb_1__vehicle " +
                                    "on tb_1_.ID = tb_1__vehicle.ID " +
                                    "where tb_1_.ID = ? and tb_1_.ASSET_TYPE = ?"
                    ).variables(800L, "CAR");
                    ctx.rows("[900]");
                }
        );
    }

    @Test
    public void testLeafJoinThroughForeignKeyDeclaredInIntermediateType() {
        CarTable table = CarTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.owner().name().eq("Car Owner"))
                        .select(table.name(), table.owner().name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME, tb_2_.NAME " +
                                    "from ML_JOINED_ASSET tb_1_ " +
                                    "inner join ML_JOINED_VEHICLE tb_1__vehicle " +
                                    "on tb_1_.ID = tb_1__vehicle.ID " +
                                    "inner join ML_JOINED_VEHICLE_OWNER tb_2_ " +
                                    "on tb_1__vehicle.OWNER_ID = tb_2_.ID " +
                                    "where tb_2_.NAME = ? and tb_1_.ASSET_TYPE = ?"
                    ).variables("Car Owner", "CAR");
                    ctx.rows("[{\"_1\":\"Joined Car\",\"_2\":\"Car Owner\"}]");
                }
        );
    }
}
