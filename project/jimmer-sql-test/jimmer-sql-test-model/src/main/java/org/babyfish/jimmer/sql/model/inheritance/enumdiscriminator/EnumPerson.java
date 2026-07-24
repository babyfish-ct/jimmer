package org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("PERSON")
public interface EnumPerson extends EnumClient {

    String firstName();
}
