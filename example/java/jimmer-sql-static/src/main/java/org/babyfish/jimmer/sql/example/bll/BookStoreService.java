package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.sql.example.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.dto.BookStoreInput;
import org.babyfish.jimmer.sql.example.model.dto.ComplexBookStore;
import org.babyfish.jimmer.sql.example.model.dto.DefaultBookStore;
import org.babyfish.jimmer.sql.example.model.dto.SimpleBookStore;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookStore")
@Transactional
public class BookStoreService {

    private final BookStoreRepository bookStoreRepository;

    public BookStoreService(BookStoreRepository bookStoreRepository) {
        this.bookStoreRepository = bookStoreRepository;
    }

    @GetMapping("/simpleList")
    public List<SimpleBookStore> findSimpleStores() {
        return bookStoreRepository.findAllStaticObjects(
                SimpleBookStore.class,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/list")
    public List<DefaultBookStore> findStores() {
        return bookStoreRepository.findAllStaticObjects(
                DefaultBookStore.class,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/{id}")
    @Nullable
    public ComplexBookStore findComplexStore(@PathVariable("id") long id) {
        return bookStoreRepository.findNullableStaticObject(ComplexBookStore.class, id);
    }

    @PutMapping
    public BookStore saveBookStore(@RequestBody BookStoreInput input) {
        return bookStoreRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBookStore(@PathVariable("id") long id) {
        bookStoreRepository.deleteById(id);
    }
}
