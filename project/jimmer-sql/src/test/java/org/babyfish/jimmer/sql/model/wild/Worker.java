package org.babyfish.jimmer.sql.model.wild;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface Worker {

    @Id
    long id();

    String name();

    @OneToMany(mappedBy = "owner")
    List<Task> tasks();
}
