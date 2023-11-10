package org.babyfish.jimmer.sql.kt.model.filter

import org.babyfish.jimmer.sql.*

@Entity
interface File {

    @Id
    val id: Long

    @Key
    val name: String

    @ManyToOne
    @Key
    @OnDissociate(DissociateAction.DELETE)
    val parent: File?

    @OneToMany(mappedBy = "parent", orderedProps = [OrderedProp("id")])
    val childFiles: List<File>

    @ManyToMany
    val users: List<User>
}