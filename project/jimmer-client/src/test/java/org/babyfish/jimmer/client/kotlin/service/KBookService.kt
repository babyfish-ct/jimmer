package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.ThrowsAll
import org.babyfish.jimmer.client.common.*
import org.babyfish.jimmer.client.kotlin.*
import org.babyfish.jimmer.client.kotlin.model.*
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
        @RequestParam("name") name: String?,
        @RequestParam("storeName") storeName: String?,
        @RequestParam("authorName") authorName: String?,
        @RequestParam(value = "minPrice", required = false) minPrice: BigDecimal,
        @RequestParam(value = "maxPrice", required = false) maxPrice: BigDecimal
    ): List<@FetchBy("COMPLEX_FETCHER") KBook>

    @Api
    @GetMapping("/tuples")
    fun findTuples(
        @RequestParam("name") name: String?,
        @RequestParam("pageIndex") pageIndex: Int,
        @RequestParam("pageSize") pageSize: Int
    ): KPage<Tuple2<out @FetchBy("COMPLEX_FETCHER") KBook, out @FetchBy("AUTHOR_FETCHER") KAuthor>>

    @Api
    @PutMapping("/book")
    @ThrowsAll(KBusinessError::class)
    fun saveBooks(@RequestBody input: KBookInput?): KBook?

    @Api
    @PatchMapping("/book")
    @ThrowsAll(KBusinessError::class)
    fun updateBook(@RequestBody input: KBookInput?): KBook?

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