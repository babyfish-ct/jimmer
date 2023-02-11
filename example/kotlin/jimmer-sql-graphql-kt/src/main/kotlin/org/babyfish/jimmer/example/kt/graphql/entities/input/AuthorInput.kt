package org.babyfish.jimmer.example.kt.graphql.entities.input

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Gender
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers

data class AuthorInput(
    val id: Long?,
    val firstName: String,
    val lastName: String,
    val gender: Gender
): Input<Author> {

    override fun toEntity(): Author =
        CONVERTER.toAuthor(this)

    @Mapper
    internal interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toAuthor(input: AuthorInput): Author
    }

    companion object {
        @JvmStatic
        private val CONVERTER = Mappers.getMapper(Converter::class.java)
    }
}