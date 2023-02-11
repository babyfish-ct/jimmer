package org.babyfish.jimmer.sql.example.model.input

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.example.model.BookStore
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers

data class BookStoreInput(
    val id: Long?,
    val name: String,
    val website: String?
): Input<BookStore> {

    override fun toEntity(): BookStore =
        CONVERTER.toBookStore(this)

    @Mapper
    internal interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        fun toBookStore(input: BookStoreInput): BookStore
    }

    companion object {
        @JvmStatic
        private val CONVERTER = Mappers.getMapper(Converter::class.java)
    }
}