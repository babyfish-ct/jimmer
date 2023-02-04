package org.babyfish.jimmer.sql.example.bll

import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.dal.BookRepository
import org.babyfish.jimmer.sql.example.model.Book
import org.babyfish.jimmer.sql.example.model.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/book")
@Transactional
class BookService(
    private val bookRepository: BookRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleBooks(): List<SimpleBook> =
        bookRepository.findAllStaticObjects(SimpleBook::class) {
            asc(Book::name)
            asc(Book::chapters)
        }

    @GetMapping("/list")
    fun findBooks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        // The `sortCode` also support implicit join, like `store.name asc`
        @RequestParam(defaultValue = "name asc, edition desc") sortCode: String,
        @RequestParam name: String?,
        @RequestParam storeName: String?,
        @RequestParam authorName: String?,
    ): Page<DefaultBook> =
        bookRepository.findBooks(
            PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
            name,
            storeName,
            authorName,
            DefaultBook::class
        )

    @GetMapping("/complexList")
    fun findComplexBooks(
        @RequestParam(defaultValue = "0") pageIndex: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        // The `sortCode` also support implicit join, like `store.name asc`
        @RequestParam(defaultValue = "name asc, edition desc") sortCode: String,
        @RequestParam name: String?,
        @RequestParam storeName: String?,
        @RequestParam authorName: String?,
    ): Page<ComplexBook> =
        bookRepository.findBooks(
            PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
            name,
            storeName,
            authorName,
            ComplexBook::class
        )

    @PutMapping
    fun saveBook(@RequestBody input: BookInput): Book =
        bookRepository.save(input)

    @PutMapping("/withChapters")
    fun saveBook(@RequestBody input: CompositeBookInput): Book =
        bookRepository.save(input)

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: Long) {
        bookRepository.deleteById(id)
    }
}
