package org.babyfish.jimmer.sql.example.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.example.model.TreeNodeTable;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ShellComponent
public class TreeQueryShell {

    private final JSqlClient sqlClient;

    private final ObjectWriter prettyWriter;

    public TreeQueryShell(JSqlClient sqlClient, ObjectWriter prettyWriter) {
        this.sqlClient = sqlClient;
        this.prettyWriter = prettyWriter;
    }

    @ShellMethod(
            "Find treeNodes, can optionally exclude child nodes of some nodes" +
            "(Example: trees --no-recursive clothing,drinks)"
    )
    public void trees(
            @ShellOption(defaultValue = "") String rootName,
            @ShellOption(defaultValue = "") String noRecursive
    ) throws JsonProcessingException {

        Set<String> noRecursiveNames;
        if (noRecursive.isEmpty()) {
            noRecursiveNames = Collections.emptySet();
        } else {
            noRecursiveNames = Arrays
                    .stream(noRecursive.trim().split("\\s*,\\s*"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }

        Fluent fluent = sqlClient.createFluent();
        TreeNodeTable treeNode = new TreeNodeTable();
        List<TreeNode> rootNodes = fluent
                .query(treeNode)
                .where(treeNode.parent().isNull())
                .whereIf(
                        !rootName.isEmpty(),
                        () -> treeNode.name().ilike(rootName)
                )
                .select(
                        treeNode.fetch(
                                TreeNodeFetcher.$
                                        .allScalarFields()
                                        .childNodes(
                                                TreeNodeFetcher.$
                                                        .allScalarFields(),
                                                it -> it
                                                        .recursive(args ->
                                                                !noRecursiveNames.contains(
                                                                        args.getEntity().name().toLowerCase()
                                                                )
                                                        )
                                                        .filter(args ->
                                                                args.orderBy(args.getTable().name())
                                                        )
                                        )
                        )
                )
                .execute();

        for (TreeNode rootNode : rootNodes) {
            System.out.println(
                    prettyWriter.writeValueAsString(rootNode)
            );
        }
    }
}
