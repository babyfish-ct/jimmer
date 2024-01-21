package io.quarkiverse.jimmer.it.service.impl;

import jakarta.enterprise.context.ApplicationScoped;

import org.babyfish.jimmer.quarkus.runtime.Jimmer;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import io.quarkiverse.jimmer.it.entity.Book;
import io.quarkiverse.jimmer.it.service.IBook;

@ApplicationScoped
public class BookImpl implements IBook {

    private final Jimmer jimmer;

    public BookImpl(Jimmer jimmer) {
        this.jimmer = jimmer;
    }

    @Override
    public Book findById(long id) {
        return jimmer.getDefaultJSqlClient().findById(Book.class, id);
    }

    @Override
    public SimpleSaveResult<Book> save(Book book) {
        SimpleSaveResult<Book> save = jimmer.getDefaultJSqlClient().save(book);
        int i = 1 / 0;
        return save;
    }
}
