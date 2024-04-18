package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.common.*
import org.babyfish.jimmer.client.kotlin.model.*
import org.babyfish.jimmer.client.kotlin.model.dto.KBookInput
import org.babyfish.jimmer.client.kotlin.model.dto.KFixedBookInput
import org.babyfish.jimmer.client.meta.Api
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import java.math.BigDecimal

/**
 * BookService interface
 */
@Api("kBookService")
interface KBookService {

    /**
     * @return A list contains simple DTOs
     */
    @Api
    @GetMapping("/books/simple")
    fun findSimpleBooks(): List<@FetchBy("SIMPLE_FETCHER") KBook>

    /**
     * @return A list contains complex DTOs
     */
    @Api
    @GetMapping("/books/complex")
    fun findComplexBooks(
        @RequestParam("name", required = false) name: String?,
        @RequestParam("storeName", required = false) storeName: String?,
        @RequestParam("authorName", required = false) authorName: String?,
        @RequestParam(value = "minPrice", required = false) minPrice: BigDecimal?,
        @RequestParam(value = "maxPrice", required = false) maxPrice: BigDecimal?
    ): List<@FetchBy("COMPLEX_FETCHER") KBook>

    @Api
    @GetMapping("/tuples")
    fun findTuples(
        @RequestParam("name", required = false) name: String?,
        @RequestParam("pageIndex") pageIndex: Int,
        @RequestParam("pageSize") pageSize: Int
    ): KPage<Tuple2<out @FetchBy("COMPLEX_FETCHER") KBook, out @FetchBy("AUTHOR_FETCHER") KAuthor>>

    @Api
    @PostMapping("/book")
    @Throws(KBusinessException::class)
    fun saveBook(@RequestBody input: KBookInput): KBook

    @Api
    @PostMapping("/book/fixed")
    @Throws(KBusinessException::class)
    fun saveBook(@RequestBody input: KFixedBookInput): KBook

    @Api
    @PutMapping("/book")
    @Throws(KBusinessException::class)
    fun updateBook(@RequestBody input: KBookInput): KBook

    @Api
    @PatchMapping("/book")
    @Throws(KBusinessException::class)
    fun patchBook(@RequestBody input: KBookInput): KBook

    @Api
    @DeleteMapping("/book/{id}")
    fun deleteBook(@PathVariable("id") id: Long): Int

    companion object {

        /**
         * Simple Book DTO
         */
        val SIMPLE_FETCHER = newFetcher(KBook::class).by {
            name()
        }

        /**
         * Complex Book DTO
         */
        @JvmStatic
        val COMPLEX_FETCHER = newFetcher(KBook::class).by {
            allScalarFields()
            store {
                allScalarFields()
            }
            authors {
                allScalarFields()
            }
        }

        /**
         * Author DTO used to be a part of return value of `BookService.findTuples`
         */
        val AUTHOR_FETCHER = newFetcher(KAuthor::class).by {
            allScalarFields()
            books {
                allScalarFields()
                store {
                    allScalarFields()
                }
            }
        }
    }
}