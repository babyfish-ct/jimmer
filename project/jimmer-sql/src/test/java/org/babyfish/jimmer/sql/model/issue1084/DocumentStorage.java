package org.babyfish.jimmer.sql.model.issue1084;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
@DatabaseValidationIgnore
public interface DocumentStorage {

    @Id
    long id();

    String fileName();

    byte[] fileContent();
}
