package org.babyfish.jimmer.kt.dto

import org.babyfish.jimmer.kt.model.*
import org.babyfish.jimmer.mapstruct.JimmerMapperConfig
import org.babyfish.jimmer.mapstruct.byDto
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers

data class BookDto(
    val name: String,
    val edition: String,
    val price: String,
    val store: TargetOfStore?,
    val authors: List<TargetOfAuthors>
) {
    data class TargetOfStore(
        val name: String
    )
    data class TargetOfAuthors(
        val firstName: String,
        val lastName: String
    )

    fun toEntity(): Book =
        MAPPER.toBook(this)

    companion object {

        @JvmStatic
        private val MAPPER = Mappers.getMapper(BookDtoMapper::class.java)
    }

    @Mapper(config = JimmerMapperConfig::class)
    abstract class BookDtoMapper {

        abstract fun fillBookDraft(dto: BookDto, @MappingTarget draft: BookDraft)

        fun toBook(dto: BookDto): Book =
            byDto(dto, this::fillBookDraft)

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        abstract fun fillBookStoreDraft(dto: TargetOfStore, @MappingTarget draft: BookStoreDraft)

        fun toBookStore(dto: TargetOfStore): BookStore =
            byDto(dto, this::fillBookStoreDraft)

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        abstract fun fillAuthorDraft(dto: TargetOfAuthors, @MappingTarget draft: AuthorDraft)

        fun toAuthor(dto: TargetOfAuthors): Author =
            byDto(dto, this::fillAuthorDraft)
    }
}