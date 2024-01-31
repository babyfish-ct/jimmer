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

    @ManyToMany(mappedBy = "vipCustomers", orderedProps = @OrderedProp("name"))
    List<Shop> vipShops();

    @ManyToMany(mappedBy = "ordinaryCustomers", orderedProps = @OrderedProp("name"))
    List<Shop> ordinaryShops();
}
