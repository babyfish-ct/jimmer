package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MultiLevelSingleTableInheritanceQueryTest extends AbstractQueryTest {

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
                                    "from ML_SINGLE_ASSET tb_1_ " +
                                    "where tb_1_.ASSET_TYPE in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK");
                    ctx.row(0, row -> {
                        assertEquals(700L, row.get_1());
                        assertEquals("Single Car", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(701L, row.get_1());
                        assertEquals("Single Truck", row.get_2());
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
                        .where(table.id().in(Arrays.asList(700L, 701L, 702L)))
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
                                    "from ML_SINGLE_ASSET tb_1_ " +
                                    "left join ML_SINGLE_ASSET tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.ASSET_TYPE in (?, ?) " +
                                    "left join ML_SINGLE_ASSET tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_3_.ASSET_TYPE = ? " +
                                    "left join ML_SINGLE_ASSET tb_4_ " +
                                    "on tb_1_.ID = tb_4_.ID and tb_4_.ASSET_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK", "CAR", "DOC", 700L, 701L, 702L);
                    ctx.row(0, row -> {
                        assertEquals(Car.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals("Single Car", row.name());
                        assertEquals("Toyota", ((Vehicle) row).manufacturer());
                        assertEquals(5, ((Car) row).seatCount());
                        assertLoadState(row, "id", "name", "manufacturer", "seatCount");
                    });
                    ctx.row(1, row -> {
                        assertEquals(Truck.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals("Single Truck", row.name());
                        assertEquals("Volvo", ((Vehicle) row).manufacturer());
                        assertLoadState(row, "id", "name", "manufacturer");
                    });
                    ctx.row(2, row -> {
                        assertEquals(Document.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals("Single Manual", row.name());
                        assertEquals("PDF", ((Document) row).format());
                        assertLoadState(row, "id", "name", "format");
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
                        .where(table.id().in(Arrays.asList(700L, 702L)))
                        .orderBy(table.id())
                        .select(table.id(), vehicle.manufacturer(), car.seatCount()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.MANUFACTURER, tb_3_.SEAT_COUNT " +
                                    "from ML_SINGLE_ASSET tb_1_ " +
                                    "left join ML_SINGLE_ASSET tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.ASSET_TYPE in (?, ?) " +
                                    "left join ML_SINGLE_ASSET tb_3_ " +
                                    "on tb_2_.ID = tb_3_.ID and tb_3_.ASSET_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("CAR", "TRUCK", "CAR", 700L, 702L);
                    ctx.row(0, row -> {
                        assertEquals(700L, row.get_1());
                        assertEquals("Toyota", row.get_2());
                        assertEquals(5, row.get_3());
                    });
                    ctx.row(1, row -> {
                        assertEquals(702L, row.get_1());
                        assertNull(row.get_2());
                        assertNull(row.get_3());
                    });
                }
        );
    }
}
