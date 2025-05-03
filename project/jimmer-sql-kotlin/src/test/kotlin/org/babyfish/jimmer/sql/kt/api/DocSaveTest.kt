package org.babyfish.jimmer.sql.kt.api

import org.babyfish.jimmer.client.Doc
import javax.sql.DataSource

@Doc(saveAstNode = true, onlySaveCurrentModuleClass = false)
class Test (
    val id: Long,
    val name: String,
    val parent: TestParent,
)

class TestParent (
    val parentId: Long,
    val ds :DataSource
)