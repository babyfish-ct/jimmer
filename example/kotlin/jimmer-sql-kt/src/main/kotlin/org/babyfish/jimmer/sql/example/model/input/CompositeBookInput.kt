package org.babyfish.jimmer.sql.example.model.input

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Book
import org.babyfish.jimmer.sql.example.model.BookStore
import org.babyfish.jimmer.sql.example.model.Gender
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers
import java.math.BigDecimal

data class CompositeBookInput(
    val id: Long?,
    val name: String,
    val edition: Int,
    val price: BigDecimal,
    val store: StoreTarget?,
    val authors: List<AuthorTarget>
): Input<Book> {

    override fun toEntity(): Book =
        CONVERTER.toBook(this)

    @Mapper
    internal interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toBook(input: CompositeBookInput): Book

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toBookStore(id: StoreTarget): BookStore

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toAuthor(id: AuthorTarget): Author
    }

    companion object {
        @JvmStatic
        private val CONVERTER = Mappers.getMapper(Converter::class.java)
    }

    data class StoreTarget(
        val name: String,
        val website: String?
    )

    data class AuthorTarget(
        val firstName: String,
        val lastName: String,
        val gender: Gender
    )
}