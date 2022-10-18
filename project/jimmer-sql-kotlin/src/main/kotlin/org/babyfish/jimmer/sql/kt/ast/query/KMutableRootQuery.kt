package org.babyfish.jimmer.sql.kt.ast.query

interface KMutableRootQuery<E: Any> : KMutableQuery<E>, KRootSelectable<E>