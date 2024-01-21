package io.quarkiverse.jimmer.it.service;

import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import io.quarkiverse.jimmer.it.entity.Book;

public interface IBook {

    Book findById(long id);

    SimpleSaveResult<Book> save(Book book);
}
