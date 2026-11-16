package com.demo.bookstore_app.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.demo.bookstore_app.models.Book;

@DataJpaTest
@ActiveProfiles("test")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void countBooksByGenreGroupsAndCounts() {
        bookRepository.save(new Book("The Little Prince", 100.0, "Antoine de Saint-Exupery", "Novel", 1943));
        bookRepository.save(new Book("Vol de nuit", 120.0, "Antoine de Saint-Exupery", "Novel", 1931));
        bookRepository.save(new Book("Clean Code", 50.0, "Robert C. Martin", "Technology", 2008));

        List<Object[]> rows = bookRepository.countBooksByGenre();

        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        assertEquals(2, counts.size());
        assertEquals(2L, counts.get("Novel").longValue());
        assertEquals(1L, counts.get("Technology").longValue());
    }

    @Test
    void findByAuthorNameIgnoresCase() {
        bookRepository.save(new Book("Clean Code", 50.0, "Robert C. Martin", "Technology", 2008));

        List<Book> result = bookRepository.findByAuthorNameContainingIgnoreCase("martin");

        assertEquals(1, result.size());
    }
}	