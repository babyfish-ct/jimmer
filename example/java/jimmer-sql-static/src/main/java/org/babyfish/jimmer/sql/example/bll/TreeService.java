package org.babyfish.jimmer.sql.example.bll;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.example.dal.TreeNodeRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.example.model.dto.RecursiveTree;
import org.babyfish.jimmer.sql.example.model.dto.RecursiveTreeInput;
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

    private final ObjectMapper objectMapper;

    public TreeService(TreeNodeRepository treeNodeRepository, ObjectMapper objectMapper) {
        this.treeNodeRepository = treeNodeRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/roots/recursive")
    public List<RecursiveTree> findRootTrees(@RequestParam(required = false) String rootName) {
        return treeNodeRepository.findByParentIsNullAndName(rootName, RecursiveTree.class);
    }

    @PutMapping("/root/recursive")
    public TreeNode saveTree(@RequestBody RecursiveTreeInput input) {

        TreeNode rootNode = input.toEntity();

        // Make sure `TreeNode.parent` is loaded because a part of key
        rootNode = TreeNodeDraft.$.produce(rootNode, draft -> draft.setParent((TreeNode) null));

        return treeNodeRepository.save(rootNode);
    }

    @DeleteMapping("/tree/{id}")
    public void deleteTree(@PathVariable long id) {
        treeNodeRepository.deleteById(id);
    }
}
