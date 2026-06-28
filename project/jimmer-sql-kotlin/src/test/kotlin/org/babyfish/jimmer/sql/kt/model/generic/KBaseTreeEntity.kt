package org.babyfish.jimmer.sql.kt.model.generic

import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OneToMany

@MappedSuperclass
interface KBaseTreeEntity<T : KBaseTreeEntity<T>> {

    @ManyToOne
    val parent: T?

    @OneToMany(mappedBy = "parent")
    val children: List<T>
}
