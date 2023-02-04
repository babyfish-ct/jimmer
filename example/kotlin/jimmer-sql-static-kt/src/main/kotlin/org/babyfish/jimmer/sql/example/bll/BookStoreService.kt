package org.babyfish.jimmer.sql.example.bll

import org.babyfish.jimmer.sql.example.dal.BookStoreRepository
import org.babyfish.jimmer.sql.example.model.BookStore
import org.babyfish.jimmer.sql.example.model.dto.BookStoreInput
import org.babyfish.jimmer.sql.example.model.dto.ComplexBookStore
import org.babyfish.jimmer.sql.example.model.dto.DefaultBookStore
import org.babyfish.jimmer.sql.example.model.dto.SimpleBookStore
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/bookStore")
@Transactional
class BookStoreService(
    private val bookStoreRepository: BookStoreRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleStores(): List<SimpleBookStore> =
        bookStoreRepository.findAllStaticObjects(SimpleBookStore::class) {
            asc(BookStore::name)
        }

    @GetMapping("/list")
    fun findStores(): List<DefaultBookStore> =
        bookStoreRepository.findAllStaticObjects(DefaultBookStore::class) {
            asc(BookStore::name)
        }

    @GetMapping("/complexList")
    fun findComplexStores(): List<ComplexBookStore> =
        bookStoreRepository.findAllStaticObjects(ComplexBookStore::class) {
            asc(BookStore::name)
        }

    @PutMapping
    fun saveBookStore(@RequestBody input: BookStoreInput): BookStore =
        bookStoreRepository.save(input)

    @DeleteMapping("{id}")
    fun deleteBookStore(@PathVariable id: Long) {
        bookStoreRepository.deleteById(id)
    }
}