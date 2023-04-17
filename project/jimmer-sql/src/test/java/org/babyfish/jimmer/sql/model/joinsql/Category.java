package org.babyfish.jimmer.sql.model.joinsql;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToMany;

import java.util.List;

@Entity
public interface Category {

    @Id
    long id();

    String name();

    @ManyToMany(mappedBy = "categories")
    List<Post> posts();
}
