package org.babyfish.jimmer.sql.model.middle;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
public interface Shop {

    @Id
    long id();

    String name();

    @ManyToMany(orderedProps = @OrderedProp("name"))
    @JoinTable(
            readonly = true,
            name = "shop_customer_mapping",
            joinColumnName = "shop_id",
            inverseJoinColumnName = "customer_id",
            logicalDeletedFilter = @JoinTable.LogicalDeletedFilter(
                    columnName = "deleted_millis",
                    type = long.class,
                    generatorType = LDValueGenerator.class
            )
    )
    List<Customer> customers();

    @ManyToMany(orderedProps = @OrderedProp("name"))
    @JoinTable(
            readonly = true,
            name = "shop_customer_mapping",
            joinColumnName = "shop_id",
            inverseJoinColumnName = "customer_id",
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
    List<Customer> vipCustomers();

    @ManyToMany(orderedProps = @OrderedProp("name"))
    @JoinTable(
            readonly = true,
            name = "shop_customer_mapping",
            joinColumnName = "shop_id",
            inverseJoinColumnName = "customer_id",
            logicalDeletedFilter = @JoinTable.LogicalDeletedFilter(
                    columnName = "deleted_millis",
                    type = long.class,
                    generatorType = LDValueGenerator.class
            ),
            filter = @JoinTable.JoinTableFilter(
                    columnName = "type",
                    values = "ORDINARY"
            )
    )
    List<Customer> ordinaryCustomers();
}
