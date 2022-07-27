package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface Country {

    @Id
    String code();

    String name();

    @OneToMany(mappedBy = "country")
    List<Author> authors();
}
