package org.babyfish.jimmer.sql.model.generic;

import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.IdView;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.OneToMany;
import org.jspecify.annotations.Nullable;

import java.util.List;

@MappedSuperclass
public interface GenericBaseTreeEntity<T extends GenericBaseTreeEntity<T>> {

    @ManyToOne
    @Nullable
    T getParent();

    @IdView("parent")
    @Nullable
    Long getParentId();

    @OneToMany(mappedBy = "parent")
    List<T> getChildren();
}
