package org.babyfish.jimmer.sql.model.middle;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
public interface Vendor {

    @Id
    long id();

    @Key
    String name();

    @ManyToMany(mappedBy = "vendors", orderedProps = @OrderedProp("name"))
    List<Shop> shops();

    @ManyToMany(orderedProps = @OrderedProp("name"))
    @JoinTable(
            name = "shop_vendor_mapping",
            joinColumnName = "vendor_id",
            inverseJoinColumnName = "shop_id",
            logicalDeletedFilter = @JoinTable.LogicalDeletedFilter(
                    columnName = "deleted_millis",
                    type = long.class,
                    generatorType = LDValueGenerator.class
            ),
            filter = @JoinTable.JoinTableFilter(
                    columnName = "type",
                    values = "VIP"
            )
    )
    List<Shop> vipShops();

    @ManyToMany(mappedBy = "ordinaryVendors", orderedProps = @OrderedProp("name"))
    List<Shop> ordinaryShops();

    @LogicalDeleted(generatorType = LDValueGenerator.class)
    long deletedMillis();
}
