package org.babyfish.jimmer.sql.model.middle;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
public interface Customer {

    @Id
    long id();

    @Key
    String name();

    @ManyToMany(mappedBy = "customers", orderedProps = @OrderedProp("name"))
    List<Shop> shops();

    @ManyToMany(orderedProps = @OrderedProp("name"))
    @JoinTable(
            name = "shop_customer_mapping",
            joinColumnName = "customer_id",
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

    @ManyToMany(mappedBy = "ordinaryCustomers", orderedProps = @OrderedProp("name"))
    List<Shop> ordinaryShops();

    @ManyToMany
    List<Card> cards();
}
