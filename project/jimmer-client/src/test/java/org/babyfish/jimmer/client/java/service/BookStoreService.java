package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.java.model.dto.ComplexBookStore;
import org.babyfish.jimmer.client.meta.common.GetMapping;
import org.babyfish.jimmer.client.meta.common.PathVariable;
import org.babyfish.jimmer.client.meta.common.RequestMapping;

@RequestMapping("/bookStore")
public interface BookStoreService {

    @GetMapping("/{id}")
    ComplexBookStore findBookStoreById(
            @PathVariable("id") long id
    );
}
