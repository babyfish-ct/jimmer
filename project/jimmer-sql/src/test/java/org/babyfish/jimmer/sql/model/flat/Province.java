package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface Province {

    @Id
    long id();

    String provinceName();

    @Nullable
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Country country();

    @OneToMany(mappedBy = "province")
    List<City> cities();
}
