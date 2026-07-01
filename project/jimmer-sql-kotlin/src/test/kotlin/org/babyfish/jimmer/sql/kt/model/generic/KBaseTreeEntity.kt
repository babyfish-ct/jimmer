package org.babyfish.jimmer.sql.kt.model.generic

import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OneToMany

@MappedSuperclass
interface KBaseTreeEntity<T : KBaseTreeEntity<T>> {

    @ManyToOne
    val parent: T?

    @IdView("parent")
    val parentId: Long?

    @OneToMany(mappedBy = "parent")
    val children: List<T>
}
