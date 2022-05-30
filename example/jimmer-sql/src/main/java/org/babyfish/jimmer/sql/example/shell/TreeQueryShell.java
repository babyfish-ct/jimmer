package org.babyfish.jimmer.sql.example.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.example.model.TreeNodeTable;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent
public class TreeQueryShell {

    private final SqlClient sqlClient;

    private final ObjectWriter prettyWriter;

    public TreeQueryShell(SqlClient sqlClient, ObjectWriter prettyWriter) {
        this.sqlClient = sqlClient;
        this.prettyWriter = prettyWriter;
    }

    @ShellMethod(
            "Find treeNodes, can optionally exclude child nodes of one node" +
            "(Example: trees --no-recursive clothing)"
    )
    public void trees(
            @ShellOption(defaultValue = "") String noRecursive
    ) throws JsonProcessingException {

        List<TreeNode> rootNodes = sqlClient
                .createQuery(TreeNodeTable.class, (q, treeNode) -> {
                    q
                            .where(treeNode.parent().isNull())
                            .orderBy(treeNode.name());
                    return q.select(
                            treeNode.fetch(
                                    TreeNodeFetcher.$
                                            .allScalarFields()
                                            .childNodes(
                                                    TreeNodeFetcher.$
                                                            .allScalarFields(),
                                                    it -> it
                                                            .filter(args -> args.orderBy(args.getTable().name()))
                                                            .recursive((node, depth) ->
                                                                    noRecursive.isEmpty() ||
                                                                            !noRecursive.equalsIgnoreCase(node.name())
                                                            )
                                            )
                            )
                    );
                })
                .execute();

        for (TreeNode rootNode : rootNodes) {
            System.out.println(
                    prettyWriter.writeValueAsString(rootNode)
            );
        }
    }
}
