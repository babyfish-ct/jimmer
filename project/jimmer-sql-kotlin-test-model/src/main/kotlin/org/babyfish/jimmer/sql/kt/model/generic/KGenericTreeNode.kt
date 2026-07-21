package org.babyfish.jimmer.sql.kt.model.generic

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "GENERIC_TREE_NODE")
interface KGenericTreeNode : KBaseNamedTreeEntity<KGenericTreeNode>
