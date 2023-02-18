package org.babyfish.jimmer.sql.example.bll.resolver;

import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.dal.BookStoreRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BookStoreNewestBooksResolver implements TransientResolver<Long, List<Long>> {

    private final BookStoreRepository bookStoreRepository;

    public BookStoreNewestBooksResolver(BookStoreRepository bookStoreRepository) {
        this.bookStoreRepository = bookStoreRepository;
    }

    @Override
    public Map<Long, List<Long>> resolve(Collection<Long> ids) {
        return bookStoreRepository.findIdAndNewestBookId(ids)
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Tuple2::get_1,
                                Collectors.mapping(
                                        Tuple2::get_2,
                                        Collectors.toList()
                                )
                        )
                );
    }
}
