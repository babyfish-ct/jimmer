package org.babyfish.jimmer.sql.example.model.input;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Data
public class RecursiveTreeInput implements Input<TreeNode> {

    private final static Converter CONVERTER = Mappers.getMapper(Converter.class);

    private String name;

    @Nullable
    private List<RecursiveTreeInput> childNodes;

    @Override
    public TreeNode toEntity() {
        return CONVERTER.toTreeNode(this);
    }

    @Mapper
    interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        TreeNode toTreeNode(RecursiveTreeInput input);
    }
}
