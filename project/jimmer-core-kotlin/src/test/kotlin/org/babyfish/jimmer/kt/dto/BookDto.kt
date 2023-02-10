package org.babyfish.jimmer.kt.dto

import org.babyfish.jimmer.kt.model.*
import org.mapstruct.*
import org.mapstruct.factory.Mappers

data class BookDto(
    val name: String?,
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

    @Mapper
    interface BookDtoMapper {

        fun toBook(dto: BookDto): Book

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toBookStore(dto: TargetOfStore): BookStore

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toAuthor(dto: TargetOfAuthors): Author
    }
}