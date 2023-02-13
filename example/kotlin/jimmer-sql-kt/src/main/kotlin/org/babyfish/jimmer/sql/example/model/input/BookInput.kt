package org.babyfish.jimmer.sql.example.model.input

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Book
import org.babyfish.jimmer.sql.example.model.BookStore
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
    val authorIds: List<Long>
): Input<Book> {

    override fun toEntity(): Book =
        CONVERTER.toBook(this)

    @Mapper
    internal interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        @Mapping(target = "store", source = "storeId")
        @Mapping(target = "authors", source = "authorIds")
        fun toBook(input: BookInput): Book

        @BeanMapping(ignoreByDefault = true)
        @Mapping(target = "id", source = ".")
        fun toBookStore(id: Long?): BookStore

        @BeanMapping(ignoreByDefault = true)
        @Mapping(target = "id", source = ".")
        fun toAuthor(id: Long?): Author
    }

    companion object {
        @JvmStatic
        private val CONVERTER = Mappers.getMapper(Converter::class.java)
    }
}