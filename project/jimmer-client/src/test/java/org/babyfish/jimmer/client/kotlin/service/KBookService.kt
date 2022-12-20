package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.client.Doc
import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.kotlin.model.*
import org.babyfish.jimmer.client.meta.common.*
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import java.math.BigDecimal

@Doc("BookService interface")
interface KBookService {

    @GetMapping("/books/simple")
    fun findSimpleBooks(): List<@FetchBy("SIMPLE_FETCHER") KBook>

    @Doc("Find book list")
    @Doc("Format of each element:")
    @Doc("- id")
    @Doc("- name")
    @Doc("- edition")
    @Doc("- price")
    @Doc("- store")
    @Doc("-- id")
    @Doc("-- name")
    @Doc("- authors")
    @Doc("-- id")
    @Doc("-- firstName")
    @GetMapping("/books/complex")
    fun findComplexBooks(
        @RequestParam("name") name: String?,
        @RequestParam("storeName") storeName: String?,
        @RequestParam("authorName") authorName: String?,
        @RequestParam(value = "minPrice", required = false) minPrice: BigDecimal,
        @RequestParam(value = "maxPrice", required = false) maxPrice: BigDecimal
    ): List<@FetchBy("COMPLEX_FETCHER") KBook>

    @GetMapping("/tuples")
    fun findTuples(
        @Doc("Match the book name, optional") @RequestParam("name") name: String?,
        @Doc("Start from 0, not 1") @RequestParam("pageIndex") pageIndex: Int,
        @RequestParam("pageSize") pageSize: Int
    ): KPage<Tuple2<out @FetchBy("COMPLEX_FETCHER") KBook, out @FetchBy("AUTHOR_FETCHER") KAuthor>>

    @PutMapping("/book")
    fun saveBooks(@RequestBody input: KBookInput?): KBook?

    @DeleteMapping("/book/{id}")
    fun deleteBook(@PathVariable("id") id: Long): Int

    companion object {
        val SIMPLE_FETCHER = newFetcher(KBook::class).by {
            name()
        }

        val COMPLEX_FETCHER = newFetcher(KBook::class).by {
            allScalarFields()
            store {
                allScalarFields()
            }
            authors {
                allScalarFields()
            }
        }

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