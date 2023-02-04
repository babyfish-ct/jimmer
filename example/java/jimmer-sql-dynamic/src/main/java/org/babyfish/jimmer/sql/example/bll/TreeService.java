package org.babyfish.jimmer.sql.example.bll;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.example.dal.TreeNodeRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.example.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.example.model.dto.RecursiveTreeInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Transactional
@RequestMapping("/tree")
public class TreeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreeService.class);

    private final TreeNodeRepository treeNodeRepository;

    public TreeService(TreeNodeRepository treeNodeRepository, ObjectMapper objectMapper) {
        this.treeNodeRepository = treeNodeRepository;
    }

    @GetMapping("/root/recursive")
    public List<@FetchBy("RECURSIVE_FETCHER") TreeNode> findRootTrees(
            @RequestParam(required = false) String rootName,
            @RequestParam(required = false) String noRecursiveNames
    ) {
        return treeNodeRepository.findByParentIsNullAndName(
                rootName,
                RECURSIVE_FETCHER
        );
    }

    @PutMapping
    public TreeNode saveTree(@RequestBody RecursiveTreeInput input) {
        TreeNode rootNode = TreeNodeDraft.$.produce(

                input.toEntity(),

                // `parent` must be loaded because it is a part of key
                draft -> draft.setParent((TreeNode) null)
        );
        return treeNodeRepository.save(rootNode);
    }

    @DeleteMapping("/{id}")
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
