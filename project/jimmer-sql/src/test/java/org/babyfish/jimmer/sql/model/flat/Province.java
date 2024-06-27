package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
public interface Province {

    @Id
    long id();

    String provinceName();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Country country();

    @OneToMany(mappedBy = "province")
    List<City> cities();
}
