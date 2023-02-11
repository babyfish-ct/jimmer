package org.babyfish.jimmer.example.kt.graphql.entities.input

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Book
import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.Chapter
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers
import java.math.BigDecimal

data class BookInput(
    val id: Long?,
    val name: String,
    val edition: Int,
    val price: BigDecimal,
    val storeId: Long?,
    val authorIds: List<Long>,
    val chapters: List<TargetOfChapters>
): Input<Book> {

    override fun toEntity(): Book =
        CONVERTER.toBook(this)

    data class TargetOfChapters(
        val index: Int,
        val title: String
    )

    @Mapper
    internal interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        @Mapping(target = "store", source = "storeId")
        @Mapping(target = "authors", source = "authorIds")
        fun toBook(input: BookInput): Book

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        @Mapping(target = "id", source = ".")
        fun toBookStore(id: Long?): BookStore

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        @Mapping(target = "id", source = ".")
        fun toAuthor(id: Long?): Author

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toChapter(input: TargetOfChapters): Chapter
    }

    companion object {
        @JvmStatic
        private val CONVERTER = Mappers.getMapper(Converter::class.java)
    }
}