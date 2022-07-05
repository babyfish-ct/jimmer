package org.babyfish.jimmer.sql.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public interface Country {

    @Id
    String code();

    String name();

    @OneToMany(mappedBy = "country")
    List<Author> authors();
}
