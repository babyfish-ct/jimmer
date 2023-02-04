package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.sql.example.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.dto.BookStoreInput;
import org.babyfish.jimmer.sql.example.model.dto.ComplexBookStore;
import org.babyfish.jimmer.sql.example.model.dto.DefaultBookStore;
import org.babyfish.jimmer.sql.example.model.dto.SimpleBookStore;
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

    @GetMapping("/complexList")
    public List<ComplexBookStore> findComplexStores() {
        return bookStoreRepository.findAllStaticObjects(
                ComplexBookStore.class,
                BookStoreProps.NAME
        );
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
