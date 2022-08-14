package org.babyfish.jimmer.example.kt.sql.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.babyfish.jimmer.example.kt.sql.model.TreeNode
import org.babyfish.jimmer.example.kt.sql.model.fetchBy
import org.babyfish.jimmer.example.kt.sql.model.name
import org.babyfish.jimmer.example.kt.sql.model.parent
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.table.isNull
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class TreeQueryShell(
    private val sqlClient: KSqlClient,
    private val prettyWriter: ObjectWriter
) {

    @ShellMethod(
        "Find treeNodes, can optionally exclude child nodes of some nodes" +
            "(Example: tree --no-recursive clothing,drinks)"
    )
    fun trees(
        @ShellOption(defaultValue = "") rootName: String,
        @ShellOption(defaultValue = "") noRecursive: String
    ) {
        val noRecursiveNames =
            if (noRecursive.isEmpty()) {
                emptySet()
            } else {
                noRecursive
                    .trim()
                    .split("\\s*,\\s*")
                    .map {
                        it.lowercase()
                    }
                    .toSet()
            }
        val rootNodes = sqlClient.createQuery(TreeNode::class) {
            where(table.parent.isNull())
            rootName.takeIf { it.isNotEmpty() }?.let {
                where(table.name ilike it)
            }
            orderBy(table.name)
            select(
                table.fetchBy {
                    allScalarFields()
                    childNodes({
                        recursive {
                            !noRecursiveNames.contains(
                                entity.name.lowercase()
                            )
                        }
                        filter {
                            orderBy(table.name)
                        }
                    }) {
                        allScalarFields()
                    }
                }
            )
        }.execute()

        for (rootNode in rootNodes) {
            println(prettyWriter.writeValueAsString(rootNode))
        }
    }
}