package org.babyfish.jimmer.sql.model.issue888;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.Table;

import java.util.List;

@Entity
@Table(name = "issue888_structure")
public interface Structure {

    @Id
    long id();

    String name();

    @OneToMany(mappedBy = "structure")
    List<Item> items();
}
