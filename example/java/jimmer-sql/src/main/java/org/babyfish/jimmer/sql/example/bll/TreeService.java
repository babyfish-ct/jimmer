package org.babyfish.jimmer.sql.example.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.example.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.example.model.TreeNodeTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class TreeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreeService.class);

    private final JSqlClient sqlClient;

    private final ObjectMapper objectMapper;

    public TreeService(JSqlClient sqlClient, ObjectMapper objectMapper) {
        this.sqlClient = sqlClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/trees")
    public List<TreeNode> trees(
            @RequestParam(defaultValue = "") String rootName,
            @RequestParam(defaultValue = "") String noRecursiveNames
    ) throws JsonProcessingException {

        Set<String> excludedNames;
        if (noRecursiveNames.isEmpty()) {
            excludedNames = Collections.emptySet();
        } else {
            excludedNames = Arrays
                    .stream(noRecursiveNames.trim().split("\\s*,\\s*"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }

        TreeNodeTable treeNode = TreeNodeTable.$;
        return sqlClient
                .createQuery(treeNode)
                .where(treeNode.parent().isNull())
                .whereIf(
                        !rootName.isEmpty(),
                        treeNode.name().ilike(rootName)
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
                                                                !excludedNames.contains(
                                                                        args.getEntity().name().toLowerCase()
                                                                )
                                                        )
                                        )
                        )
                )
                .execute();
    }

    @PutMapping("/tree")
    public TreeNode saveTree(
            @RequestParam String rootName,
            @RequestParam(defaultValue = "2") int depth,
            @RequestParam(defaultValue = "2") int breadth
    ) throws JsonProcessingException {

        if (rootName.isEmpty()) {
            throw new IllegalArgumentException("Illegal rootName");
        }
        if (depth > 5) {
            throw new IllegalArgumentException("depth is too big");
        }
        if (breadth > 5) {
            throw new IllegalArgumentException("breadth is too big");
        }

        TreeNode rootNode = TreeNodeDraft.$.produce(rootDraft -> {
            rootDraft.setName(rootName).setParent((TreeNode) null);
            createChildNodes(
                    rootDraft,
                    0,
                    depth,
                    breadth
            );
        });

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "save the tree " +
                            objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(rootNode) +
                            " into database"
            );
        }
        return sqlClient
                .getEntities()
                .saveCommand(rootNode)
                .configure(
                        // If child nodes do not exist, insert them automatically
                        it -> it.setAutoAttachingAll()

                        // Need not `setAutoDetaching` because `on cascade delete`
                        // is configured on model interface,
                        // If user update a tree with less child nodes,
                        // abandoned child nodes will be deleted automatically
                )
                .execute()
                .getModifiedEntity();
    }

    private static void createChildNodes(
            TreeNodeDraft parent,
            int currentDepth,
            int maxDepth,
            int childCount
    ) {
        if (currentDepth >= maxDepth) {
            parent.setChildNodes(Collections.emptyList());
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

    @DeleteMapping("/tree/{id}")
    public void deleteTree(@PathVariable long id) {
        sqlClient.getEntities().delete(TreeNode.class, id);
    }
}
