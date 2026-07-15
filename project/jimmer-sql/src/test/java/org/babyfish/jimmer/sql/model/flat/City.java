package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.json.JsonConverter;
import org.babyfish.jimmer.json.LongToStringConverter;
import org.babyfish.jimmer.sql.*;

@Entity
public interface City {

    @Id
    @JsonConverter(LongToStringConverter.class)
    long id();

    String cityName();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Province province();
}
