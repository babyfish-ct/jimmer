package org.babyfish.jimmer.sql.example.model.input

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.example.model.TreeNode
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers

data class RecursiveTreeInput(
    val name: String,
    val childNodes: List<RecursiveTreeInput>?
): Input<TreeNode> {

    override fun toEntity(): TreeNode =
        CONVERTER.toTreeNode(this)

    @Mapper
    internal interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toTreeNode(input: RecursiveTreeInput): TreeNode
    }

    companion object {
        @JvmStatic
        private val CONVERTER = Mappers.getMapper(Converter::class.java)
    }
}