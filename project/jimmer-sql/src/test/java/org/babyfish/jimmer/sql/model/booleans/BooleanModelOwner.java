package org.babyfish.jimmer.sql.model.booleans;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import java.util.List;
import java.util.UUID;

@Entity
public interface BooleanModelOwner {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @OneToMany(mappedBy = "owner")
    List<BooleanModel> models();
}

