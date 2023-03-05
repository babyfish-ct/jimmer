package org.babyfish.jimmer.sql.example.bll;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.ThrowsAll;
import org.babyfish.jimmer.sql.example.dal.TreeNodeRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.example.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.example.model.input.RecursiveTreeInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;
import org.babyfish.jimmer.sql.runtime.SaveErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@RestController`, not `@Service`.
 */
@RestController
@Transactional
@RequestMapping("/tree")
public class TreeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreeService.class);

    private final TreeNodeRepository treeNodeRepository;

    public TreeService(TreeNodeRepository treeNodeRepository, ObjectMapper objectMapper) {
        this.treeNodeRepository = treeNodeRepository;
    }

    @GetMapping("/roots/recursive")
    public List<@FetchBy("RECURSIVE_FETCHER") TreeNode> findRootTrees(
            @RequestParam(required = false) String rootName
    ) {
        return treeNodeRepository.findByParentIsNullAndName(
                rootName,
                RECURSIVE_FETCHER
        );
    }

    @PutMapping("/root/recursive")
    @ThrowsAll(SaveErrorCode.class)
    public TreeNode saveTree(@RequestBody RecursiveTreeInput input) {
        TreeNode rootNode = TreeNodeDraft.$.produce(

                input.toEntity(),

                /*
                 * `TreeNode` has two key properties: `name` and `parent`,
                 * this means `name` and `parent` must be specified when `id` is missing.
                 *
                 * One-to-many association is special, parent object can specify the
                 * many-to-one association of its child objects implicitly.
                 * In this demo, Associations named `childNodes` specify `parent`
                 * for child objects implicitly so that all child objects do not require
                 * the `parent`.
                 *
                 * However, the `parent` of ROOT cannot be specified implicitly,
                 * so that it must be specified manually
                 */
                draft -> draft.setParent(null)
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
