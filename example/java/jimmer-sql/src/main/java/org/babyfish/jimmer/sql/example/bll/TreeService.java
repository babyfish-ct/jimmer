package org.babyfish.jimmer.sql.example.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.example.dal.TreeNodeRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.example.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class TreeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreeService.class);

    private final TreeNodeRepository treeNodeRepository;

    private final ObjectMapper objectMapper;

    public TreeService(TreeNodeRepository treeNodeRepository, ObjectMapper objectMapper) {
        this.treeNodeRepository = treeNodeRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/rootNodes")
    public List<TreeNode> findRootTrees(
            @RequestParam(required = false) String rootName
    ) {
        return treeNodeRepository.findRootNodes(rootName, null);
    }

    @GetMapping("/rootTrees")
    public List<@FetchBy("RECURSIVE_FETCHER") TreeNode> findRootTrees(
            @RequestParam(required = false) String rootName,
            @RequestParam(required = false) String noRecursiveNames
    ) {
        if (!StringUtils.hasText(noRecursiveNames)) {
            return treeNodeRepository.findRootNodes(rootName, RECURSIVE_FETCHER);
        }

        Set<String> excludedNames;
        if (noRecursiveNames.isEmpty()) {
            excludedNames = Collections.emptySet();
        } else {
            excludedNames = Arrays
                    .stream(noRecursiveNames.trim().split("\\s*,\\s*"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
        return treeNodeRepository.findRootNodes(
                rootName,
                TreeNodeFetcher.$from(RECURSIVE_FETCHER) // override `RECURSIVE_TREE_NODE_FETCHER`
                        .childNodes(
                                TreeNodeFetcher.$.allScalarFields(),
                                cfg -> cfg.recursive(strategy ->
                                    !excludedNames.contains(
                                            strategy.getEntity().name().toLowerCase()
                                    )
                                )
                        )
        );
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
        return treeNodeRepository.save(rootNode);
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
        treeNodeRepository.deleteById(id);
    }

    private static final Fetcher<TreeNode> RECURSIVE_FETCHER =
            TreeNodeFetcher.$
                    .allScalarFields()
                    .childNodes(
                            TreeNodeFetcher.$.allScalarFields(),
                            RecursiveListFieldConfig::recursive
                    );
}
