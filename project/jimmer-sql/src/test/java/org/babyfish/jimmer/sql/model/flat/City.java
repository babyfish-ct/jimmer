package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.*;

@Entity
@DatabaseValidationIgnore
public interface City {

    @Id
    @JsonConverter(LongToStringConverter.class)
    long id();

    String cityName();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Province province();
}
