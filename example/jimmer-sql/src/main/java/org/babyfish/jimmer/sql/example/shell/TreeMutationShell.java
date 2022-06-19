package org.babyfish.jimmer.sql.example.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeDraft;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class TreeMutationShell {

    private final SqlClient sqlClient;

    private final ObjectWriter prettyWriter;

    public TreeMutationShell(SqlClient sqlClient, ObjectWriter prettyWriter) {
        this.sqlClient = sqlClient;
        this.prettyWriter = prettyWriter;
    }

    @ShellMethod("Save tree into database(Example: save-tree --root-name Hello)")
    public void saveTree(
            String rootName,
            @ShellOption(defaultValue = "2") int depth,
            @ShellOption(defaultValue = "2") int breadth
    ) throws JsonProcessingException {
        TreeNode rootNode = TreeNodeDraft.$.produce(rootDraft -> {
            rootDraft.setName(rootName).setParent((TreeNode) null);
            createChildNodes(
                    rootDraft,
                    0,
                    depth,
                    breadth
            );
        });
        System.out.println(
                "save the tree " +
                        prettyWriter.writeValueAsString(rootNode) +
                        " into database"
        );
        sqlClient
                .getEntities()
                .saveCommand(rootNode)
                .configure(
                        // If child nodes do not exist, create them
                        it -> it.setAutoAttachingAll()

                        // Need not `setAutoDetaching` because `on cascade delete`
                        // is configured on model interface
                )
                .execute();
    }

    private static void createChildNodes(
            TreeNodeDraft parent,
            int currentDepth,
            int maxDepth,
            int childCount
    ) {
        if (currentDepth >= maxDepth) {
            return;
        }
        String prefix = parent.name() + "-";
        for (int i = 1; i <= childCount; i++) {
            String childName = prefix + i;
            parent.addIntoChildNodes(childDraft -> {
                childDraft.setName(childName);
                createChildNodes(
                        childDraft,
                        currentDepth + 1,
                        maxDepth,
                        childCount
                );
            });
        }
    }
}
